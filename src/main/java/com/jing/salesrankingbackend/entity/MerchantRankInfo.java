package com.jing.salesrankingbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 排行榜数据统计表实体
 */
@Data
@TableName("merchant_rank_info")
public class MerchantRankInfo {

    /** 自增id */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 城市id，全国为 000000 */
    private String cityId;

    /** 排行榜类型：0爆款榜 1飙升榜 */
    @TableField("`type`")
    private Integer type;

    /** 类目类型：0全部类型 1美食 2休闲丽人 */
    private Integer category;

    /** 商家id */
    private Long merchantId;

    /** 排名 */
    private Integer sort;

    /** 月售数量 */
    private Integer saleNumMonth;

    /** 日售数量 */
    private Integer saleNumDay;

    /** 统计日期 */
    @TableField("`date`")
    private String date;

    /** 0未删除 */
    @TableLogic
    private Long isDelete;

    /** 创建时间 */
    private Long createTime;

    /** 更新时间 */
    private Long updateTime;
}
