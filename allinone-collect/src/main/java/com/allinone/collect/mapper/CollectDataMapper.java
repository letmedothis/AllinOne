package com.allinone.collect.mapper;

import com.allinone.collect.domain.CollectData;
import com.allinone.collect.domain.CollectDataCell;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CollectDataMapper {
    List<CollectData> selectCollectDataList(CollectData data);
    CollectData selectCollectDataById(Long dataId);
    int insertCollectData(CollectData data);
    int updateCollectData(CollectData data);
    int updateCollectDataStatus(CollectData data);
    int deleteCollectDataById(Long dataId);
    int deleteCollectDataByIds(Long[] dataIds);

    /** 模板删除级联：查询模板下仍为草稿的填报数据 ID */
    List<Long> selectDraftDataIdsByTemplateIds(@Param("templateIds") Long[] templateIds);

    /** 模板删除级联：逻辑删除模板下全部草稿数据 */
    int deleteDraftDataByTemplateIds(@Param("templateIds") Long[] templateIds);
}

