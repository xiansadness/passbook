package com.lx.passbook.service.impl;

import com.alibaba.fastjson.JSON;
import com.lx.passbook.constant.Constants;
import com.lx.passbook.mapper.PassTemplateRowMapper;
import com.lx.passbook.service.IGainPassTemplateService;
import com.lx.passbook.utils.RowKeyGenUtil;
import com.lx.passbook.vo.GainPassTemplateRequest;
import com.lx.passbook.vo.PassTemplate;
import com.lx.passbook.vo.Response;
import com.spring4all.spring.boot.starter.hbase.api.HbaseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 用户领取优惠券功能实现
 */

@Slf4j
@Service
public class GainPassTemplateServiceImpl implements IGainPassTemplateService {

    /** HBase 客户端 */
    private final HbaseTemplate hbaseTemplate;

    /** redis 客户端 */
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public GainPassTemplateServiceImpl(HbaseTemplate hbaseTemplate,
                                       StringRedisTemplate redisTemplate) {
        this.hbaseTemplate = hbaseTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Response gainPassTemplate(GainPassTemplateRequest request) throws Exception {

        PassTemplate passTemplate;
        //构造优惠券id即rowKey
        String passTemplateId = RowKeyGenUtil.genPassTemplateRowKey(
                request.getPassTemplate());

        try {//从数据库中获得要领取的优惠券的信息
            passTemplate = hbaseTemplate.get(
                    Constants.PassTemplateTable.TABLE_NAME,//表名
                    passTemplateId,//行键即rowKey
                    new PassTemplateRowMapper()
            );
        } catch (Exception ex) {
            log.error("Gain PassTemplate Error: {}",
                    JSON.toJSONString(request.getPassTemplate()));
            return Response.failure("Gain PassTemplate Error!");
        }

        //判断优惠券的库存
        if (passTemplate.getLimit() <= 1 && passTemplate.getLimit() != -1) {
            log.error("PassTemplate Limit Max: {}",
                    JSON.toJSONString(request.getPassTemplate()));
            return Response.failure("PassTemplate Limit Max!");
        }

        //校验领取时间
        Date cur = new Date();
        if (!(cur.getTime() >= passTemplate.getStart().getTime()
                && cur.getTime() < passTemplate.getEnd().getTime())) {
            log.error("PassTemplate ValidTime Error: {}",
                    JSON.toJSONString(request.getPassTemplate()));
            return Response.failure("PassTemplate ValidTime Error!");
        }

        // 减去优惠券的 limit，更改优惠券信息表中的库存列
        if (passTemplate.getLimit() != -1) {//limit=-1,说明优惠券没有上限，可以不用修改

            List<Mutation> datas = new ArrayList<>();
            byte[] FAMILY_C = Constants.PassTemplateTable.FAMILY_C.getBytes();
            byte[] LIMIT = Constants.PassTemplateTable.LIMIT.getBytes();

            Put put = new Put(Bytes.toBytes(passTemplateId));
            put.addColumn(FAMILY_C, LIMIT,
                    Bytes.toBytes(passTemplate.getLimit() - 1));
            datas.add(put);

            hbaseTemplate.saveOrUpdates(Constants.PassTemplateTable.TABLE_NAME,
                    datas);
        }

        // 将优惠券保存到用户优惠券表
        if (!addPassForUser(request, passTemplate.getId(), passTemplateId)) {
            return Response.failure("GainPassTemplate Failure!");
        }

        return Response.success();
    }

    /**
     * <h2>给用户添加优惠券</h2>
     * @param request {@link GainPassTemplateRequest} 该请求中包含了用户id和优惠券详细信息
     * @param merchantsId 商户 id
     * @param passTemplateId 优惠券 id
     * @return true/false
     * */
    //本质上为向pass表中添加记录
    private boolean addPassForUser(GainPassTemplateRequest request,
                                   Integer merchantsId, String passTemplateId) throws Exception {

        //定义pass表中列族和字段的字节数组
        byte[] FAMILY_I = Constants.PassTable.FAMILY_I.getBytes();
        byte[] USER_ID = Constants.PassTable.USER_ID.getBytes();
        byte[] TEMPLATE_ID = Constants.PassTable.TEMPLATE_ID.getBytes();
        byte[] TOKEN = Constants.PassTable.TOKEN.getBytes();
        byte[] ASSIGNED_DATE = Constants.PassTable.ASSIGNED_DATE.getBytes();
        byte[] CON_DATE = Constants.PassTable.CON_DATE.getBytes();

        List<Mutation> datas = new ArrayList<>();
        Put put = new Put(Bytes.toBytes(RowKeyGenUtil.genPassRowKey(request)));
        put.addColumn(FAMILY_I, USER_ID, Bytes.toBytes(request.getUserId()));
        put.addColumn(FAMILY_I, TEMPLATE_ID, Bytes.toBytes(passTemplateId));

        if (request.getPassTemplate().getHasToken()) {
            //从redis中取出token
            String token = redisTemplate.opsForSet().pop(passTemplateId);
            if (null == token) {
                log.error("Token not exist: {}", passTemplateId);
                return false;
            }
            //将token写入文件
            recordTokenToFile(merchantsId, passTemplateId, token);
            put.addColumn(FAMILY_I, TOKEN, Bytes.toBytes(token));
        } else {
            //-1表示token不存在
            put.addColumn(FAMILY_I, TOKEN, Bytes.toBytes("-1"));
        }

        put.addColumn(FAMILY_I, ASSIGNED_DATE,
                Bytes.toBytes(DateFormatUtils.ISO_DATE_FORMAT.format(new Date())));
        put.addColumn(FAMILY_I, CON_DATE, Bytes.toBytes("-1"));

        datas.add(put);

        hbaseTemplate.saveOrUpdates(Constants.PassTable.TABLE_NAME, datas);

        return true;
    }

    /**
     * <h2>将已使用的 token 记录到文件中</h2>
     * @param merchantsId 商户 id
     * @param passTemplateId 优惠券 id
     * @param token 分配的优惠券 token
     * */
    private void recordTokenToFile(Integer merchantsId, String passTemplateId,
                                   String token) throws Exception {

        Files.write(
                Paths.get(Constants.TOKEN_DIR, String.valueOf(merchantsId),
                        passTemplateId + Constants.USED_TOKEN_SUFFIX),//token文件目录
                (token + "\n").getBytes(),//该路径文件填充的内容
                StandardOpenOption.CREATE, StandardOpenOption.APPEND//以追加的方式写入
        );
    }
}
