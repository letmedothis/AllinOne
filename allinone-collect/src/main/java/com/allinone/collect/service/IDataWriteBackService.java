package com.allinone.collect.service;

import com.allinone.collect.domain.CollectData;

public interface IDataWriteBackService {
    void writeBack(Long dataId);
    void writeBack(CollectData data);
}
