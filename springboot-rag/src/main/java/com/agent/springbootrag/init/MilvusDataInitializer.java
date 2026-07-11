package com.agent.springbootrag.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Milvus 数据初始化 —— SDS 化学品安全领域测试数据
 * <p>仅首次启动时写入，后续启动自动跳过</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
@Profile("dev")
public class MilvusDataInitializer implements ApplicationRunner {

    private final VectorStore milvus;

    @Override
    public void run(ApplicationArguments args) {
        if (args.containsOption("skip-init")) {
            log.info("跳过 Milvus 数据初始化（--skip-init）");
            return;
        }

        // 检查是否已有数据，避免重复写入
        if (hasExistingData()) {
            log.info("Milvus 已有 SDS 数据，跳过初始化");
            return;
        }

        List<Document> docs = List.of(
                doc("SDS（Safety Data Sheet）即安全数据表，包含16个标准章节，用于描述化学品的理化特性、危害信息及安全操作规范。",
                        "sds-overview", "SDS"),

                doc("甲醇（Methanol）属于第3类易燃液体，UN编号1230。主要危害：高度易燃，吞食有毒，可致失明。GHS标签要素包括火焰、骷髅交叉骨和健康危害符号。",
                        "hazard-methanol", "危险性概述"),
                doc("盐酸（Hydrochloric Acid）属于第8类腐蚀性物质，UN编号1789。主要危害：造成严重皮肤灼伤和眼损伤，吸入有害。浓度大于25%时归类为腐蚀性。",
                        "hazard-hcl", "危险性概述"),
                doc("氢氧化钠（Sodium Hydroxide）属于第8类腐蚀性物质，UN编号1823。固体和溶液均具有强腐蚀性，可造成严重灼伤。操作时必须佩戴防化学品手套和护目镜。",
                        "hazard-naoh", "危险性概述"),

                doc("甲醇皮肤接触急救：立即脱去污染衣物，用大量肥皂水冲洗皮肤至少15分钟。如出现刺激症状，就医。",
                        "firstaid-methanol-skin", "急救措施"),
                doc("甲醇吸入急救：迅速转移至空气新鲜处，保持呼吸舒适体位。如呼吸困难，给予吸氧。如呼吸停止，立即进行人工呼吸并就医。",
                        "firstaid-methanol-inhalation", "急救措施"),
                doc("盐酸溅入眼睛急救：立即用大量清水冲洗眼睛至少15分钟，翻开上下眼睑确保充分冲洗。冲洗后立即就医，告知医生接触化学品名称。",
                        "firstaid-hcl-eye", "急救措施"),

                doc("甲醇灭火：可使用抗溶性泡沫、干粉、二氧化碳灭火器。禁止使用直流水柱，可能导致液体飞溅扩大火势。消防人员须佩戴正压自给式呼吸器。",
                        "fire-methanol", "消防措施"),

                doc("甲醇储存要求：存放于阴凉通风处，远离热源和火源。储存温度不超过30°C，容器保持密闭。与氧化剂、酸类分开存放。库区应配备防爆电气设备。",
                        "storage-methanol", "操作处置与储存"),

                doc("甲醇职业接触限值：中国MAC为50mg/m³，美国OSHA PEL为200ppm（TWA）。操作时须佩戴化学安全防护眼镜、防有机蒸气半面罩呼吸器、耐化学品手套。",
                        "ppe-methanol", "个体防护"),

                doc("甲醇理化特性：无色透明液体，有酒精气味。沸点64.7°C，闪点11°C（闭杯），自燃温度464°C。与水完全混溶，蒸气密度1.11（空气=1）。爆炸极限6.0%-36.5%（V/V）。",
                        "physical-methanol", "理化特性"),

                doc("甲醇稳定性：常温常压下稳定。应避免接触强氧化剂（如高锰酸钾、铬酸）、强酸和碱金属。分解产物包括甲醛和一氧化碳。",
                        "stability-methanol", "稳定性和反应性"),

                doc("甲醇毒理学：口服LD50（大鼠）5628mg/kg。主要靶器官为视神经和中枢神经系统。代谢产物甲醛和甲酸是导致失明和酸中毒的主要原因。最小致死量约为100mL。",
                        "toxicology-methanol", "毒理学信息"),

                doc("甲醇运输信息：UN编号1230，正确运输名称为甲醇（METHANOL），危险类别3（易燃液体），包装类别II。海运须符合IMDG规则，空运须符合IATA-DGR。",
                        "transport-methanol", "运输信息")
        );

        milvus.add(docs);
        log.info("Milvus 初始化完成，写入 {} 条 SDS 文档", docs.size());
    }

    private boolean hasExistingData() {
        try {
            List<Document> results = milvus.similaritySearch(
                    SearchRequest.builder().query("SDS").topK(1).build()
            );
            return results != null && !results.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private Document doc(String content, String id, String section) {
        return new Document(content, Map.of(
                "source", "milvus",
                "docId", id,
                "section", section
        ));
    }
}
