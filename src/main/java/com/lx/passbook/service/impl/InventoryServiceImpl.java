package com.lx.passbook.service.impl;

import com.lx.passbook.constant.Constants;
import com.lx.passbook.dao.MerchantsDao;
import com.lx.passbook.entity.Merchants;
import com.lx.passbook.mapper.PassTemplateRowMapper;
import com.lx.passbook.service.IInventoryService;
import com.lx.passbook.service.IUserPassService;
import com.lx.passbook.utils.RowKeyGenUtil;
import com.lx.passbook.vo.*;
import com.spring4all.spring.boot.starter.hbase.api.HbaseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.LongComparator;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 获取库存信息，只返回用户没有领取的
 */

@Slf4j
@Service
public class InventoryServiceImpl implements IInventoryService {

    /** Hbase 客户端 */
    private final HbaseTemplate hbaseTemplate;

    /** Merchants Dao 接口 */
    private final MerchantsDao merchantsDao;

    private final IUserPassService userPassService;

    @Autowired
    public InventoryServiceImpl(HbaseTemplate hbaseTemplate,
                                MerchantsDao merchantsDao,
                                IUserPassService userPassService) {
        this.hbaseTemplate = hbaseTemplate;
        this.merchantsDao = merchantsDao;
        this.userPassService = userPassService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Response getInventoryInfo(Long userId) throws Exception {

        //获取目标用户已领取的所有优惠券
        Response allUserPass = userPassService.getUserAllPassInfo(userId);

        //passInfo中包含了优惠券领取记录，优惠券自身详细信息，优惠券投放商户信息
        List<PassInfo> passInfos = (List<PassInfo>) allUserPass.getData();

        //这里只需要优惠券自身详细信息，故需要从passInfo中将该信息提取出来
        List<PassTemplate> excludeObject = passInfos.stream().map(PassInfo::getPassTemplate)
                .collect(Collectors.toList());
        List<String> excludeIds = new ArrayList<>();

        //根据提取出来的优惠券对象，获取优惠券id
        excludeObject.forEach(e -> excludeIds.add(
                RowKeyGenUtil.genPassTemplateRowKey(e)));

        //先封装可领优惠券，再添加商户信息
        return new Response(new InventoryResponse(userId,
                buildPassTemplateInfo(getAvailablePassTemplate(excludeIds))));
    }

    /**
     * <h2>获取系统中可用的优惠券</h2>
     * @param excludeIds 需要排除的优惠券 ids 目标用户已领取的优惠券的所有id
     * @return {@link PassTemplate}
     * */
    private List<PassTemplate> getAvailablePassTemplate(List<String> excludeIds) {

        //设置过滤器列表
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        //下面设置的过滤器，只要通过一个过滤条件即可，即每个过滤器之间是or的关系

        filterList.addFilter(//设置优惠券的数量条件
                new SingleColumnValueFilter(//limit的值大于0
                        Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C),//找到相应列族
                        Bytes.toBytes(Constants.PassTemplateTable.LIMIT),//列族中相应列
                        CompareFilter.CompareOp.GREATER,
                        new LongComparator(0L)
                )
        );
        filterList.addFilter(
                new SingleColumnValueFilter(//limit的值为-1，表示不设置领取上限
                        Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C),
                        Bytes.toBytes(Constants.PassTemplateTable.LIMIT),
                        CompareFilter.CompareOp.EQUAL,
                        Bytes.toBytes("-1")
                )
        );

        Scan scan = new Scan();
        scan.setFilter(filterList);

        //从优惠券信息表中找出所有未领完的优惠券
        List<PassTemplate> validTemplates = hbaseTemplate.find(
                Constants.PassTemplateTable.TABLE_NAME, scan, new PassTemplateRowMapper());

        List<PassTemplate> availablePassTemplates = new ArrayList<>();

        Date cur = new Date();

        //从所有可领优惠券中 除掉 目标用户已经领过的优惠券(根据传过来的参数来除去)
        for (PassTemplate validTemplate : validTemplates) {

            //根据rowKey来比较优惠券id
            if (excludeIds.contains(RowKeyGenUtil.genPassTemplateRowKey(validTemplate))) {
                continue;
            }

            //判断优惠券是否过期
            //当前时间 大于 当前遍历到的优惠券的领取时间 且小于结束领取时间，则说明目标用户可以领取该优惠券
            //则将其添加到能够领取的优惠券列表中去
            if (cur.getTime() >= validTemplate.getStart().getTime()
                    && cur.getTime() <= validTemplate.getEnd().getTime()) {
                availablePassTemplates.add(validTemplate);
            }
        }

        //最后返回用户可领优惠券列表
        return availablePassTemplates;
    }

    /**
     * <h2>构造优惠券的信息</h2>
     * @param passTemplates {@link PassTemplate}
     * @return {@link PassTemplateInfo}
     * */
    private
    List<PassTemplateInfo> buildPassTemplateInfo(List<PassTemplate> passTemplates) {

        //商户信息
        Map<Integer, Merchants> merchantsMap = new HashMap<>();

        List<Integer> merchantsIds = passTemplates.stream().map(
                PassTemplate::getId
        ).collect(Collectors.toList());

        List<Merchants> merchants = merchantsDao.findByIdIn(merchantsIds);
        merchants.forEach(m -> merchantsMap.put(m.getId(), m));

        List<PassTemplateInfo> result = new ArrayList<>(passTemplates.size());

        for (PassTemplate passTemplate : passTemplates) {

            Merchants mc = merchantsMap.getOrDefault(passTemplate.getId(),
                    null);
            if (null == mc) {
                log.error("Merchants Error: {}", passTemplate.getId());
                continue;
            }

            result.add(new PassTemplateInfo(passTemplate, mc));
        }

        //result中封装了优惠券自身的详细信息和投放该优惠券的商户的详细信息
        return result;
    }
}
