package com.ctrip.zeus.service.model.handler.impl;

import com.ctrip.zeus.dal.core.*;
import com.ctrip.zeus.exceptions.ValidationException;
import com.ctrip.zeus.model.entity.*;
import com.ctrip.zeus.service.model.handler.SlbSync;
import com.ctrip.zeus.support.C;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.unidal.dal.jdbc.DalException;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author:xingchaowang
 * @date: 3/7/2015.
 */
@Component("dbSync")
public class SlbSyncImpl implements SlbSync {
    @Resource
    private AppSlbDao appSlbDao;
    @Resource
    private SlbDao slbDao;
    @Resource
    private SlbDomainDao slbDomainDao;
    @Resource
    private SlbServerDao slbServerDao;
    @Resource
    private SlbVipDao slbVipDao;
    @Resource
    private SlbVirtualServerDao slbVirtualServerDao;

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public SlbDo add(Slb slb) throws DalException, ValidationException {
        validate(slb);
        SlbDo d = C.toSlbDo(slb);
        d.setCreatedTime(new Date());
        d.setVersion(1);

        slbDao.insert(d);
        cascadeSync(d, slb);
        return d;
    }

    @Override
    public SlbDo update(Slb slb) throws DalException, ValidationException {
        validate(slb);
        SlbDo d = C.toSlbDo(slb);
        slbDao.updateByName(d, SlbEntity.UPDATESET_FULL);

        SlbDo updated = slbDao.findByName(d.getName(), SlbEntity.READSET_FULL);
        d.setId(updated.getId());
        d.setVersion(updated.getVersion());
        cascadeSync(d, slb);
        return d;
    }

    @Override
    public int delete(String slbName) throws DalException, ValidationException {
        SlbDo d = slbDao.findByName(slbName, SlbEntity.READSET_FULL);
        if (d == null)
            return 0;
        if(removable(d)) {
            slbVipDao.deleteBySlb(new SlbVipDo().setSlbId(d.getId()));
            slbServerDao.deleteBySlb(new SlbServerDo().setSlbId(d.getId()));
            for (SlbVirtualServerDo svsd : slbVirtualServerDao.findAllBySlb(d.getId(), SlbVirtualServerEntity.READSET_FULL)) {
                deleteSlbVirtualServer(svsd.getId());
            }
            return slbDao.deleteByPK(d);
        }
        throw new ValidationException(slbName + " cannot be deleted. Dependency exists");
    }

    private void validate(Slb slb) throws ValidationException {
        if (slb == null) {
            throw new ValidationException("Slb with null value cannot be persisted.");
        }
    }

    private boolean removable(SlbDo d) throws DalException {
        List<AppSlbDo> list = appSlbDao.findAllBySlb(d.getName(), AppSlbEntity.READSET_FULL);
        if (list.size() == 0)
            return true;
        return false;
    }

    private void cascadeSync(SlbDo d, Slb slb) throws DalException {
        syncSlbVips(d.getId(), slb.getVips());
        syncSlbServers(d.getId(), slb.getSlbServers());
        syncVirtualServers(d.getId(), slb.getVirtualServers());
    }

    private void syncSlbVips(long slbId, List<Vip> vips) throws DalException {
        if (vips == null || vips.size() == 0)
            return;
        List<SlbVipDo> oldList = slbVipDao.findAllBySlb(slbId, SlbVipEntity.READSET_FULL);
        Map<String, SlbVipDo> oldMap = Maps.uniqueIndex(oldList, new Function<SlbVipDo, String>() {
            @Override
            public String apply(SlbVipDo input) {
                return input.getIp();
            }
        });

        //Update existed if necessary, and insert new ones.
        for (Vip e : vips) {
            SlbVipDo old = oldMap.get(e.getIp());
            if (old != null) {
                oldList.remove(old);
            }
            slbVipDao.insert(C.toSlbVipDo(e).setSlbId(slbId).setCreatedTime(new Date()));
        }

        //Remove unused ones.
        for (SlbVipDo d : oldList) {
            slbVipDao.deleteByPK(new SlbVipDo().setId(d.getId()));
        }
    }

    private void syncSlbServers(long slbId, List<SlbServer> slbServers) throws DalException {
        if (slbServers == null || slbServers.size() == 0) {
            logger.warn("No slb server is given when adding/updating slb with id " + slbId);
            return;
        }
        List<SlbServerDo> oldList = slbServerDao.findAllBySlb(slbId, SlbServerEntity.READSET_FULL);
        Map<String, SlbServerDo> oldMap = Maps.uniqueIndex(oldList, new Function<SlbServerDo, String>() {
            @Override
            public String apply(SlbServerDo input) {
                return input.getIp();
            }
        });

        //Update existed if necessary, and insert new ones.
        for (SlbServer e : slbServers) {
            SlbServerDo old = oldMap.get(e.getIp());
            if (old != null) {
                oldList.remove(old);
            }
            slbServerDao.insert(C.toSlbServerDo(e).setSlbId(slbId).setCreatedTime(new Date()));
        }

        //Remove unused ones.
        for (SlbServerDo d : oldList) {
            slbServerDao.deleteByPK(new SlbServerDo().setId(d.getId()));
        }
    }

    private void syncVirtualServers(long slbId, List<VirtualServer> virtualServers) throws DalException {
        if (virtualServers == null || virtualServers.size() == 0)
            return;
        List<SlbVirtualServerDo> oldList = slbVirtualServerDao.findAllBySlb(slbId,SlbVirtualServerEntity.READSET_FULL);
        Map<String, SlbVirtualServerDo> oldMap = Maps.uniqueIndex(oldList, new Function<SlbVirtualServerDo, String>() {
            @Override
            public String apply(SlbVirtualServerDo input) {
                return input.getName();
            }
        });

        //Update existed if necessary, and insert new ones.
        for (VirtualServer e : virtualServers) {
            SlbVirtualServerDo old = oldMap.get(e.getName());
            if (old != null) {
                oldList.remove(old);
            }
            SlbVirtualServerDo d = C.toSlbVirtualServerDo(e).setSlbId(slbId).setCreatedTime(new Date());
            slbVirtualServerDao.insert(d);

            //Domain
            syncSlbDomain(d.getId(), e.getDomains());
        }

        //Remove unused ones.
        for (SlbVirtualServerDo d : oldList) {
            deleteSlbVirtualServer(d.getId());
        }
    }

    private void syncSlbDomain(long slbVirtualServerId, List<Domain> domains) throws DalException {
        if (domains == null || domains.size() == 0)
            return;
        List<SlbDomainDo> oldList = slbDomainDao.findAllBySlbVirtualServer(slbVirtualServerId, SlbDomainEntity.READSET_FULL);
        Map<String, SlbDomainDo> oldMap = Maps.uniqueIndex(oldList, new Function<SlbDomainDo, String>() {
            @Override
            public String apply(SlbDomainDo input) {
                return input.getName();
            }
        });

        //Update existed if necessary, and insert new ones.
        for (Domain e : domains) {
            SlbDomainDo old = oldMap.get(e.getName());
            if (old != null) {
                oldList.remove(old);
            }
            slbDomainDao.insert(C.toSlbDomainDo(e).setSlbVirtualServerId(slbVirtualServerId).setCreatedTime(new Date()));
        }

        //Remove unused ones.
        for (SlbDomainDo d : oldList) {
            slbDomainDao.deleteByPK(new SlbDomainDo().setId(d.getId()));
        }
    }

    private void deleteSlbVirtualServer(long id) throws DalException {
        slbDomainDao.deleteAllBySlbVirtualServer(new SlbDomainDo().setSlbVirtualServerId(id));
        slbVirtualServerDao.deleteByPK(new SlbVirtualServerDo().setId(id));
    }
}