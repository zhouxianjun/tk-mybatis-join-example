package com.alone.tk.mybatis.test;

import com.alone.tk.mybatis.JoinExample;
import com.alone.tk.mybatis.test.entity.Store;
import com.alone.tk.mybatis.test.entity.User;
import com.alone.tk.mybatis.test.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TkMybatisTestApplicationTests {
    @Resource
    private UserMapper userMapper;

    @Test
    public void contextLoads() {
    }

    @Test
    public void pageListMap() {
        Object o = PageHelper.startPage(1, 10).doSelectPageInfo(() -> userMapper.selectByJoinExample(
                JoinExample.builder(User.class)
                        .addCol(User::getUsername)
                        .addCol(Store::getCode, User::getCompanyCode)
                        .addCol(User.class)
                        .addTable(new JoinExample.Table(Store.class, JoinExample.JoinType.JOIN, User::getCompanyCode, Store::getCode).and(User::getType, 1))
                        .where(JoinExample.Where.custom().andEqualTo(Store::getType, 1))
                        .where(new User().setCompanyCode("12345"))
                        .groupBy(Store::getCode)
                        .desc(User::getId)
                        .asc(User::getUsername)
                        .build()
        ));
        System.out.println(o);
    }

    @Test
    public void listTransform() {
        List<User> users = userMapper.selectByJoinExampleTransform(
                JoinExample.builder(User.class).build(),
                map -> new User()
        );
        System.out.println(users);
    }

    @Test
    public void entity() {
        List<User> users = userMapper.selectByJoinExampleEntity(JoinExample.builder(User.class).build());
        System.out.println(users);
    }

    @Test
    public void entityOne() {
        User user = userMapper.selectByJoinExampleEntityOne(JoinExample.builder(User.class).build());
        System.out.println(user);
    }

    @Test
    public void count() {
        int count = userMapper.selectByJoinExampleTransform(
                JoinExample
                        .builder(User.class)
                        .count()
                        .build(),
                int.class
        );
        System.out.println(count);
    }
}
