package com.alone.tk.mybatis.test.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Data
@Table(name = "t_user")
@Accessors(chain = true)
public class User implements Serializable {
    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 员工编号
     */
    private String code;

    /**
     * 机构编码
     */
    private String companyCode;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 手机号码
     */
    private Long phone;
    /**
     * 手机串号
     */
    private String imei;

    /**
     * 0-普通；1-系统管理员；2-企业管理员；3-销售业务人员；4-配送员
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
