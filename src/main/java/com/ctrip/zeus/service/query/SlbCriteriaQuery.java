package com.ctrip.zeus.service.query;

import com.ctrip.zeus.service.model.IdVersion;
import com.ctrip.zeus.service.model.ModelMode;

import java.util.Set;

/**
 * Created by zhoumy on 2015/8/27.
 */
public interface SlbCriteriaQuery {

    Set<Long> queryAll() throws Exception;

    Set<IdVersion> queryAll(ModelMode mode) throws Exception;

    Long queryByName(String name) throws Exception;

    Set<IdVersion> queryByIdsAndMode(Long[] slbIds, ModelMode mode) throws Exception;

    IdVersion[] queryByIdAndMode(Long slbId, ModelMode mode) throws Exception;

    Set<Long> queryByVs(IdVersion vsIdVersion) throws Exception;

    Set<Long> queryByVses(IdVersion[] vsIdVersions) throws Exception;

    Set<IdVersion> queryBySlbServerIp(String ip) throws Exception;
}
