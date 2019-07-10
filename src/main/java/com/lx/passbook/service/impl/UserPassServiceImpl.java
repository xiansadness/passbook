package com.lx.passbook.service.impl;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.constant.Constants;
import com.lx.passbook.constant.PassStatus;
import com.lx.passbook.dao.MerchantsDao;
import com.lx.passbook.entity.Merchants;
import com.lx.passbook.mapper.PassRowMapper;
import com.lx.passbook.service.IUserPassService;
import com.lx.passbook.service.IUserService;
import com.lx.passbook.vo.Pass;
import com.lx.passbook.vo.PassInfo;
import com.lx.passbook.vo.PassTemplate;
import com.lx.passbook.vo.Response;
import com.spring4all.spring.boot.starter.hbase.api.HbaseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserPassServiceImpl implements IUserPassService {

    /** Hbase 客户端 */
    private final HbaseTemplate hbaseTemplate;

    /** Merchants Dao */
    private final MerchantsDao merchantsDao;

    @Autowired
    public UserPassServiceImpl(HbaseTemplate hbaseTemplate, MerchantsDao merchantsDao) {
        this.hbaseTemplate = hbaseTemplate;
        this.merchantsDao = merchantsDao;
    }

    @Override
    public Response getUserPassInfo(Long userId) throws Exception {
        return getPassInfoByStatus(userId, PassStatus.UNUSED);
    }

    @Override
    public Response getUserUsedPassInfo(Long userId) throws Exception {
        return getPassInfoByStatus(userId, PassStatus.USED);
    }

    @Override
    public Response getUserAllPassInfo(Long userId) throws Exception {
        return getPassInfoByStatus(userId, PassStatus.ALL);
    }

    @Override
    public Response userUsePass(Pass pass) {

        // 根据 userId 构造行键前缀, 用于找到目标优惠券
        byte[] rowPrefix = Bytes.toBytes(new StringBuilder(
                String.valueOf(pass.getUserId())).reverse().toString());

        //用于从Hbase服务器上扫描需要的数据
        Scan scan = new Scan();

        //设置过滤器，相当于为查询语句 添加查询条件
        List<Filter> filters = new ArrayList<>();
        filters.add(new PrefixFilter(rowPrefix));//前缀过滤器
        filters.add(new SingleColumnValueFilter(//单列过滤器
                Constants.PassTable.FAMILY_I.getBytes(),
                Constants.PassTable.TEMPLATE_ID.getBytes(),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes(pass.getTemplateId())//根据优惠券的id，从用户领取的优惠券表中找到相应的优惠券信息
        ));
        filters.add(new SingleColumnValueFilter(
                Constants.PassTable.FAMILY_I.getBytes(),
                Constants.PassTable.CON_DATE.getBytes(),
                CompareFilter.CompareOp.EQUAL,
                Bytes.toBytes("-1")//当前传入的优惠券信息，必须是用户未使用过的优惠券
        ));

        scan.setFilter(new FilterList(filters));//为scan添加上面设置的过滤器

        //连接数据库，获取结果集
        List<Pass> passes = hbaseTemplate.find(Constants.PassTable.TABLE_NAME,
                scan, new PassRowMapper());

        if (null == passes || passes.size() != 1) {//未找到目标优惠券或者找到了多条目标优惠券会报错
            log.error("UserUsePass Error: {}", JSON.toJSONString(pass));
            return Response.failure("UserUsePass Error");
        }

        byte[] FAMILY_I = Constants.PassTable.FAMILY_I.getBytes();
        byte[] CON_DATE = Constants.PassTable.CON_DATE.getBytes();

        //如果找到了目标优惠券，则修改Pass表中的相关记录
        //主要是修改con_date(消费日期)这一列数据，将原来的-1，改为当前日期
        List<Mutation> datas = new ArrayList<>();//容纳put操作
        Put put = new Put(passes.get(0).getRowKey().getBytes());
        put.addColumn(FAMILY_I, CON_DATE,
                Bytes.toBytes(DateFormatUtils.ISO_DATE_FORMAT.format(new Date()))
        );
        datas.add(put);

        hbaseTemplate.saveOrUpdates(Constants.PassTable.TABLE_NAME, datas);

        return Response.success();
    }

    /**
     * <h2>根据优惠券状态获取优惠券信息</h2>
     * @param userId 用户 id
     * @param status {@link PassStatus}
     * @return {@link Response}
     * */
    private Response getPassInfoByStatus(Long userId, PassStatus status) throws Exception {

        // 根据 userId 构造行键前缀
        // 该行键前缀用作查询的条件，行键前缀相同的记录表示是同一用户所领取的优惠券
        byte[] rowPrefix = Bytes.toBytes(new StringBuilder(String.valueOf(userId)).reverse().toString());

        //定义比较过滤器
        CompareFilter.CompareOp compareOp =
                status == PassStatus.UNUSED ?
                        CompareFilter.CompareOp.EQUAL : CompareFilter.CompareOp.NOT_EQUAL;

        //用于扫描Hbase服务器上的数据
        Scan scan = new Scan();

        //先创建过滤器 再添加到 scan 上
        List<Filter> filters = new ArrayList<>();

        // 1. 行键前缀过滤器, 找到特定用户的优惠券(所有优惠券)
        filters.add(new PrefixFilter(rowPrefix));
        // 2. 基于列单元值的过滤器, 找到未使用的优惠券
        if (status != PassStatus.ALL) {//不是查询所有的时候 才设置列过滤器
            filters.add(
                    new SingleColumnValueFilter(
                            Constants.PassTable.FAMILY_I.getBytes(),
                            Constants.PassTable.CON_DATE.getBytes(), compareOp,
                            Bytes.toBytes("-1"))//消费日期为-1，则表示未使用；不为-1，则表示已使用
            );
        }
        //为scan过滤器
        scan.setFilter(new FilterList(filters));

        //获得查询结果集，并转化为pass格式
        List<Pass> passes = hbaseTemplate.find(Constants.PassTable.TABLE_NAME, scan, new PassRowMapper());

        //获取结果集中优惠券的详细信息
        Map<String, PassTemplate> passTemplateMap = buildPassTemplateMap(passes);
        //获取结果集中与优惠券相关的商铺的详细信息
        Map<Integer, Merchants> merchantsMap = buildMerchantsMap(
                new ArrayList<>(passTemplateMap.values()));

        //封装所有信息
        //passInfo: pass + passTemplate + merchant
        List<PassInfo> result = new ArrayList<>();

        for (Pass pass : passes) {

            //获取优惠券详细信息
            PassTemplate passTemplate = passTemplateMap.getOrDefault(
                    pass.getTemplateId(), null);//获取不到时，设置为null,正常情况下不会发生该状况
            if (null == passTemplate) {
                log.error("PassTemplate Null : {}", pass.getTemplateId());
                continue;
            }

            //获取商户详细信息
            Merchants merchants = merchantsMap.getOrDefault(passTemplate.getId(), null);
            if (null == merchants) {
                log.error("Merchants Null : {}", passTemplate.getId());
                continue;
            }

            //填充result
            result.add(new PassInfo(pass, passTemplate, merchants));
        }

        return new Response(result);
    }

    /**
     * <h2>通过获取的 Passes 对象构造 PassTemplates Map</h2>
     * @param passes {@link Pass} 用户所领取的优惠券
     * @return Map {@link PassTemplate}
     * */
    //用户所领取的优惠券（pass表）到每个优惠券的详细信息（passTemplate）
    private
    Map<String, PassTemplate> buildPassTemplateMap(List<Pass> passes) throws Exception {

        String[] patterns = new String[] {"yyyy-MM-dd"};

        byte[] FAMILY_B = Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B);
        byte[] ID = Bytes.toBytes(Constants.PassTemplateTable.ID);
        byte[] TITLE = Bytes.toBytes(Constants.PassTemplateTable.TITLE);
        byte[] SUMMARY = Bytes.toBytes(Constants.PassTemplateTable.SUMMARY);
        byte[] DESC = Bytes.toBytes(Constants.PassTemplateTable.DESC);
        byte[] HAS_TOKEN = Bytes.toBytes(Constants.PassTemplateTable.HAS_TOKEN);
        byte[] BACKGROUND = Bytes.toBytes(Constants.PassTemplateTable.BACKGROUND);

        byte[] FAMILY_C = Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C);
        byte[] LIMIT = Bytes.toBytes(Constants.PassTemplateTable.LIMIT);
        byte[] START = Bytes.toBytes(Constants.PassTemplateTable.START);
        byte[] END = Bytes.toBytes(Constants.PassTemplateTable.END);

        //先从pass列表中 获取每一个pass中 包含的 所领取的 优惠券的id，从而获取优惠券id列表templateIds
        List<String> templateIds = passes.stream().map(
                Pass::getTemplateId
        ).collect(Collectors.toList());

        //相当于是构造批量Get对象，Get用来获取Hbase表中的相关记录
        List<Get> templateGets = new ArrayList<>(templateIds.size());//先初始化一个用来容纳Get方法的列表
        //对列表templateIds中存在的每一个id，都需要将id作为参数传给Get对象，
        // 从而在passTemplate表中根据优惠券id来获得优惠券的详细信息
        templateIds.forEach(t -> templateGets.add(new Get(Bytes.toBytes(t))));

        //获得批量Get操作的结果集
        Result[] templateResults = hbaseTemplate.getConnection()//先连接数据库
                .getTable(TableName.valueOf(Constants.PassTemplateTable.TABLE_NAME))//根据表名获得表对象
                .get(templateGets);//在该表上进行get操作,相当于是查询操作？

        // 构造 PassTemplateId -> PassTemplate Object 的 Map, 用于构造 PassInfo
        Map<String, PassTemplate> templateId2Object = new HashMap<>();
        for (Result item : templateResults) {
            PassTemplate passTemplate = new PassTemplate();//先初始化一个passTemplate对象，用来转换结果集中每一条记录

            passTemplate.setId(Bytes.toInt(item.getValue(FAMILY_B, ID)));
            passTemplate.setTitle(Bytes.toString(item.getValue(FAMILY_B, TITLE)));
            passTemplate.setSummary(Bytes.toString(item.getValue(FAMILY_B, SUMMARY)));
            passTemplate.setDesc(Bytes.toString(item.getValue(FAMILY_B, DESC)));
            passTemplate.setHasToken(Bytes.toBoolean(item.getValue(FAMILY_B, HAS_TOKEN)));
            passTemplate.setBackground(Bytes.toInt(item.getValue(FAMILY_B, BACKGROUND)));

            passTemplate.setLimit(Bytes.toLong(item.getValue(FAMILY_C, LIMIT)));
            passTemplate.setStart(DateUtils.parseDate(
                    Bytes.toString(item.getValue(FAMILY_C, START)), patterns));
            passTemplate.setEnd(DateUtils.parseDate(
                    Bytes.toString(item.getValue(FAMILY_C, END)), patterns
            ));

            templateId2Object.put(Bytes.toString(item.getRow()), passTemplate);
            //key:该优惠券记录的RowKey?
            //value: passTemplate对象（包含了优惠券的详细信息）
        }

        return templateId2Object;
    }

    /**
     * <h2>通过获取的 PassTemplate 对象构造 Merchants Map</h2>
     * @param passTemplates {@link PassTemplate}
     * @return {@link Merchants}
     * */
    //通过优惠券详细信息表中的商户id，在优惠券详细信息表和商户信息表之间建立联系
    private
    Map<Integer, Merchants> buildMerchantsMap(List<PassTemplate> passTemplates) {

        Map<Integer, Merchants> merchantsMap = new HashMap<>();

        //从每一个PassTemplate中提取出商户id，并放入merchantsIds列表
        List<Integer> merchantsIds = passTemplates.stream().map(
                PassTemplate::getId
        ).collect(Collectors.toList());

        //根据商户id列表获取商户对象列表，一个商户对象便包含了该商户的详细信息
        List<Merchants> merchants = merchantsDao.findByIdIn(merchantsIds);

        //将商户对象列表中每一个元素放入商户map中
        //商户id为key,该id所对应的对象为value
        merchants.forEach(m -> merchantsMap.put(m.getId(), m));

        return merchantsMap;
    }


}
