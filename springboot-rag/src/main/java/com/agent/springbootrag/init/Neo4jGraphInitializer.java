package com.agent.springbootrag.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Neo4j 图谱数据初始化 —— SDS 化学品安全知识图谱
 * <p>仅首次启动时写入，后续启动自动跳过</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
@Profile("dev")
public class Neo4jGraphInitializer implements ApplicationRunner {

    private final Neo4jClient neo4jClient;

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("skip-init")) {
            log.info("跳过 Neo4j 数据初始化（--skip-init）");
            return;
        }

        // 检查是否已有数据，避免重复写入
        if (hasExistingData()) {
            log.info("Neo4j 已有 SDS 图谱数据，跳过初始化");
            return;
        }

        // 1. 化学品节点
        neo4jClient.query("""
            MERGE (m:Chemical {name: '甲醇'})
            SET m.cas = '67-56-1', m.formula = 'CH3OH',
                m.unNumber = '1230', m.hazardClass = 3,
                m.glassPoint = 11, m.boilingPoint = 64.7,
                m.toxicity = 'high', m.flammable = true
            
            MERGE (hcl:Chemical {name: '盐酸'})
            SET hcl.cas = '7647-01-0', hcl.formula = 'HCl',
                hcl.unNumber = '1789', hcl.hazardClass = 8,
                hcl.corrosive = true, hcl.concentration = '37%'
            
            MERGE (naoh:Chemical {name: '氢氧化钠'})
            SET naoh.cas = '1310-73-2', naoh.formula = 'NaOH',
                naoh.unNumber = '1823', naoh.hazardClass = 8,
                naoh.corrosive = true, naoh.alias = '烧碱'
            
            MERGE (h2so4:Chemical {name: '硫酸'})
            SET h2so4.cas = '7664-93-9', h2so4.formula = 'H2SO4',
                h2so4.unNumber = '1830', h2so4.hazardClass = 8,
                h2so4.corrosive = true, h2so4.oxidizing = false
        """).run();

        // 2. 危害类型节点
        neo4jClient.query("""
            MERGE (h1:Hazard {type: '易燃', ghsCode: 'GHS02', signal: '危险'})
            SET h1.name = '易燃'
            MERGE (h2:Hazard {type: '急性毒性', ghsCode: 'GHS06', signal: '危险'})
            SET h2.name = '急性毒性'
            MERGE (h3:Hazard {type: '腐蚀性', ghsCode: 'GHS05', signal: '危险'})
            SET h3.name = '腐蚀性'
            MERGE (h4:Hazard {type: '健康危害', ghsCode: 'GHS08', signal: '警告'})
            SET h4.name = '健康危害'
        """).run();

        // 3. 防护用品节点
        neo4jClient.query("""
            MERGE (p1:PPE {name: '化学安全防护眼镜', category: '眼部防护'})
            MERGE (p2:PPE {name: '防有机蒸气半面罩', category: '呼吸防护'})
            MERGE (p3:PPE {name: '耐化学品手套', category: '手部防护'})
            MERGE (p4:PPE {name: '防酸碱工作服', category: '身体防护'})
            MERGE (p5:PPE {name: '正压自给式呼吸器', category: '呼吸防护'})
        """).run();

        // 4. 法规节点
        neo4jClient.query("""
            MERGE (r1:Regulation {name: 'GHS全球化学品统一分类和标签制度', org: 'UN'})
            MERGE (r2:Regulation {name: '中国危险化学品安全管理条例', org: '国务院'})
            MERGE (r3:Regulation {name: 'OSHA职业接触限值', org: '美国OSHA'})
        """).run();

        // 5. 关系：化学品 → 危害
        neo4jClient.query("""
            MATCH (m:Chemical {name: '甲醇'}), (h1:Hazard {type: '易燃'}), (h2:Hazard {type: '急性毒性'})
            MERGE (m)-[:HAS_HAZARD {severity: 'high'}]->(h1)
            MERGE (m)-[:HAS_HAZARD {severity: 'high'}]->(h2)
            
            MATCH (hcl:Chemical {name: '盐酸'}), (h3:Hazard {type: '腐蚀性'})
            MERGE (hcl)-[:HAS_HAZARD {severity: 'high'}]->(h3)
            
            MATCH (naoh:Chemical {name: '氢氧化钠'}), (h3:Hazard {type: '腐蚀性'})
            MERGE (naoh)-[:HAS_HAZARD {severity: 'high'}]->(h3)
        """).run();

        // 6. 关系：化学品 → 防护用品
        neo4jClient.query("""
            MATCH (m:Chemical {name: '甲醇'})
            MATCH (p1:PPE {name: '化学安全防护眼镜'})
            MATCH (p2:PPE {name: '防有机蒸气半面罩'})
            MATCH (p3:PPE {name: '耐化学品手套'})
            MERGE (m)-[:REQUIRES_PPE]->(p1)
            MERGE (m)-[:REQUIRES_PPE]->(p2)
            MERGE (m)-[:REQUIRES_PPE]->(p3)
            
            MATCH (hcl:Chemical {name: '盐酸'})
            MATCH (p1:PPE {name: '化学安全防护眼镜'})
            MATCH (p4:PPE {name: '防酸碱工作服'})
            MATCH (p5:PPE {name: '正压自给式呼吸器'})
            MERGE (hcl)-[:REQUIRES_PPE]->(p1)
            MERGE (hcl)-[:REQUIRES_PPE]->(p4)
            MERGE (hcl)-[:REQUIRES_PPE]->(p5)
        """).run();

        // 7. 关系：化学品互不相容
        neo4jClient.query("""
            MATCH (m:Chemical {name: '甲醇'}), (h2so4:Chemical {name: '硫酸'})
            MERGE (m)-[:INCOMPATIBLE_WITH {reason: '强放热反应，可能引发火灾'}]->(h2so4)
            
            MATCH (naoh:Chemical {name: '氢氧化钠'}), (hcl:Chemical {name: '盐酸'})
            MERGE (naoh)-[:INCOMPATIBLE_WITH {reason: '剧烈中和反应，大量放热'}]->(hcl)
        """).run();

        // 8. 关系：化学品 → 法规
        neo4jClient.query("""
            MATCH (m:Chemical {name: '甲醇'}), (r1:Regulation {name: 'GHS全球化学品统一分类和标签制度'})
            MERGE (m)-[:REGULATED_BY]->(r1)
            
            MATCH (m:Chemical {name: '甲醇'}), (r2:Regulation {name: '中国危险化学品安全管理条例'})
            MERGE (m)-[:REGULATED_BY]->(r2)
            
            MATCH (m:Chemical {name: '甲醇'}), (r3:Regulation {name: 'OSHA职业接触限值'})
            MERGE (m)-[:REGULATED_BY {limit: '200ppm TWA'}]->(r3)
        """).run();

        log.info("Neo4j 图谱初始化完成：4 种化学品, 4 种危害, 5 种防护用品, 3 项法规");
    }

    private boolean hasExistingData() {
        try {
            Optional<Long> count = neo4jClient
                    .query("MATCH (c:Chemical) RETURN count(c) AS cnt")
                    .fetchAs(Long.class)
                    .one()
                    .map(cnt -> cnt);
            return count.orElse(0L) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
