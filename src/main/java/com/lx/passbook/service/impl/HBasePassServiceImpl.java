package com.lx.passbook.service.impl;

import com.lx.passbook.constant.Constants;
import com.lx.passbook.service.IHBasePassService;
import com.lx.passbook.utils.RowKeyGenUtil;
import com.lx.passbook.vo.PassTemplate;
import com.spring4all.spring.boot.starter.hbase.api.HbaseTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HBasePassServiceImpl implements IHBasePassService {

    //HBase客户端，用于执行数据库的增删改查
    private final HbaseTemplate hbaseTemplate;

    @Autowired
    public HBasePassServiceImpl(HbaseTemplate hbaseTemplate) {
        this.hbaseTemplate = hbaseTemplate;
    }

    @Override
    public boolean dropPassTemplateToHBase(PassTemplate passTemplate) {

        if(null == passTemplate){//数据为空，则返回false
            return false;
        }

        //根据商户id和优惠券title生成一个rowKey
        //同一个商户在投放优惠券时，需要保证每次投放优惠券的title都不同
        String rowKey = RowKeyGenUtil.genPassTemplateRowKey(passTemplate);

        try{//先创建连接，再根据表名获取表，再在表中查询rowKey
            if(hbaseTemplate.getConnection().getTable(TableName.valueOf(Constants.PassTemplateTable.TABLE_NAME))
                    .exists(new Get(Bytes.toBytes(rowKey)))){
                log.warn("RowKey {} is already exist!", rowKey);
                return false;
            }

        }catch(Exception ex){
            log.error("DropPassTemplateToHBase Error: {}", ex.getMessage());
            return false;
        }

        Put put=new Put(Bytes.toBytes(rowKey));

        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),//列族
                Bytes.toBytes(Constants.PassTemplateTable.ID),//该列族中某一列的名称
                Bytes.toBytes(passTemplate.getId())//列族中该列要填充的值
        );

        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),
                Bytes.toBytes(Constants.PassTemplateTable.TITLE),
                Bytes.toBytes(passTemplate.getTitle())
        );

        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),
                Bytes.toBytes(Constants.PassTemplateTable.SUMMARY),
                Bytes.toBytes(passTemplate.getSummary())
        );
        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),
                Bytes.toBytes(Constants.PassTemplateTable.DESC),
                Bytes.toBytes(passTemplate.getDesc())
        );
        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),
                Bytes.toBytes(Constants.PassTemplateTable.HAS_TOKEN),
                Bytes.toBytes(passTemplate.getHasToken())
        );
        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_B),
                Bytes.toBytes(Constants.PassTemplateTable.BACKGROUND),
                Bytes.toBytes(passTemplate.getBackground())
        );

        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C),
                Bytes.toBytes(Constants.PassTemplateTable.LIMIT),
                Bytes.toBytes(passTemplate.getLimit())
        );
        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C),
                Bytes.toBytes(Constants.PassTemplateTable.START),
                Bytes.toBytes(DateFormatUtils.ISO_DATE_FORMAT.format(passTemplate.getStart()))
        );
        put.addColumn(
                Bytes.toBytes(Constants.PassTemplateTable.FAMILY_C),
                Bytes.toBytes(Constants.PassTemplateTable.END),
                Bytes.toBytes(DateFormatUtils.ISO_DATE_FORMAT.format(passTemplate.getEnd()))
        );

        //入库
        hbaseTemplate.saveOrUpdate(Constants.PassTemplateTable.TABLE_NAME,put);

        return true;
    }
}













