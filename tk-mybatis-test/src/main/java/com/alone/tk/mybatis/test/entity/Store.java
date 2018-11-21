package com.alone.tk.mybatis.test.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Table(name = "t_store")
@Accessors(chain = true)
public class Store implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;

    /**
     * 上级机构Id
     */
    private Integer parentId;

    /**
     * 名称
     */
    private String name;

    /**
     * 助记码
     */
    private String alias;

    /**
     * 联系人
     */
    @Column(name = "contact_name")
    private String contactName;

    /**
     * 电话
     */
    @Column(name = "contact_tel")
    private String contactTel;

    /**
     * 地址
     */
    private String address;

    /**
     * 配送费
     */
    @Column(name = "delivery_fee")
    private Integer deliveryFee;

    /**
     * 免配送费额度
     */
    @Column(name = "free_dlv_limit")
    private Integer freeDlvLimit;

    /**
     * 销售人员Id
     */
    @Column(name = "seller_id")
    private Integer sellerId;

    /**
     * 销售人员名称
     */
    @Transient
    private Integer sellerName;

    /**
     * 分成占比
     */
    @Column(name = "share_rate")
    private Integer shareRate;

    /**
     * 分成到期日期
     */
    @Column(name = "share_end_date")
    private Date shareEndDate;

    /**
     * 配货时间:用一个字节的后7位表示周一到周日
     */
    @Column(name = "delivery_date")
    private Integer deliveryDate;

    /**
     * 操作员ID
     */
    @Column(name = "operator_id")
    private Integer operatorId;

    /**
     * 操作员姓名
     */
    @Column(name = "operator_name")
    private String operatorName;

    /**
     * 类型: 1-总公司;2-城市仓;3-前置仓
     */
    private Integer type;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    private static final long serialVersionUID = 1L;

}
