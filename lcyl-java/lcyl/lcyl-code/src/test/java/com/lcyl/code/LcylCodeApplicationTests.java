package com.lcyl.code;

import com.lcyl.code.domain.NursingItem;
import com.lcyl.code.mapper.NursingItemMapper;
import com.lcyl.system.domain.LcRoomType;
import com.lcyl.system.mapper.LcRoomTypeMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;

@SpringBootTest(classes = LcylCodeApplicationTests.TestConfig.class)
class LcylCodeApplicationTests {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"com.lcyl"})
    @MapperScan("com.lcyl.**.mapper")
    static class TestConfig {
    }

    @Resource
    private NursingItemMapper nursingItemMapper;

    @Resource
    private LcRoomTypeMapper lcRoomTypeMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 确保 lc_nurse 表有一条记录，否则 creator_id 外键报错 */
    private Long getCreatorId() {
        java.util.List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM lc_nurse ORDER BY id LIMIT 1", Long.class);
        if (ids != null && !ids.isEmpty()) {
            return ids.get(0);
        }
        // lc_nurse 表结构: id, name, phone, user_id, create_time（无 status 列）
        jdbcTemplate.update(
                "INSERT INTO lc_nurse (name, phone, user_id) VALUES ('系统管理员', '13900000001', 1)");
        Long newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        System.out.println("lc_nurse 表为空，已自动创建护士记录，id=" + newId);
        return newId;
    }

    @Test
    void insertNursingItems() {
        Long creatorId = getCreatorId();

        Object[][] items = {
                {"日常护理", 100.00, "次", 1L, "/images/service1.png", "包括洗漱、穿衣、进食等日常照料服务"},
                {"康复训练", 200.00, "小时", 2L, "/images/service2.png", "专业康复师一对一指导康复训练"},
                {"中医理疗", 150.00, "次", 3L, "/images/service3.png", "针灸、推拿、拔罐等中医理疗服务"},
                {"陪同就医", 80.00, "次", 4L, "/images/service4.png", "专人陪同老人前往医院就诊"},
                {"心理疏导", 120.00, "小时", 5L, "/images/service5.png", "专业心理咨询师提供心理疏导服务"},
        };

        for (Object[] item : items) {
            String itemName = (String) item[0];

            Integer count = nursingItemMapper.countItemName(itemName);
            if (count != null && count > 0) {
                System.out.println("已存在，跳过: " + itemName);
                continue;
            }

            NursingItem ni = new NursingItem();
            ni.setItemName(itemName);
            ni.setPrice((Double) item[1]);
            ni.setUnit((String) item[2]);
            ni.setSort((Long) item[3]);
            ni.setImageUrl((String) item[4]);
            ni.setItemDesc((String) item[5]);
            ni.setStatus(1L);
            ni.setCreatorId(creatorId);
            ni.setCreatorName("test_admin");

            nursingItemMapper.insertNursingItem(ni);
            System.out.println("插入成功: " + itemName + "，id=" + ni.getId());
        }
    }

    @Test
    void insertRoomTypes() {
        Object[][] items = {
                {"温馨双人间", 2, 2800.00, "/images/room1.png", "宽敞明亮的双人间，配备独立卫生间、空调、电视、紧急呼叫系统，适合喜欢结伴同住的老人"},
                {"舒适单人间", 1, 4500.00, "/images/room2.png", "私密舒适的单人间，独立卫浴、全自动护理床、智能呼叫系统，享受安静无忧的个人空间"},
                {"豪华套房", 2, 6800.00, "/images/room3.png", "一室一厅豪华套房，全智能家居、独立阳台、全景落地窗，尊享高品质养老生活"},
        };

        for (Object[] item : items) {
            String name = (String) item[0];

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lc_room_type WHERE name = ?", Integer.class, name);
            if (count != null && count > 0) {
                System.out.println("房型已存在，跳过: " + name);
                continue;
            }

            LcRoomType roomType = new LcRoomType();
            roomType.setName(name);
            roomType.setBedCount((Integer) item[1]);
            roomType.setPrice(BigDecimal.valueOf((Double) item[2]));
            roomType.setPhoto((String) item[3]);
            roomType.setIntroduction((String) item[4]);
            roomType.setStatus(1);
            roomType.setCreateBy("test_admin");

            lcRoomTypeMapper.insertLcRoomType(roomType);
            System.out.println("房型插入成功: " + name + "，id=" + roomType.getId());
        }
    }
}
