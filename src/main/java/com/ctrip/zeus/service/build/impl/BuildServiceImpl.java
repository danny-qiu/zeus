package com.ctrip.zeus.service.build.impl;

import com.ctrip.zeus.service.build.BuildInfoService;
import com.ctrip.zeus.service.build.BuildService;
import com.ctrip.zeus.service.build.NginxConfService;
import org.springframework.stereotype.Service;
import org.unidal.dal.jdbc.DalException;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author:xingchaowang
 * @date: 3/15/2015.
 */
@Service("buildService")
public class BuildServiceImpl implements BuildService {
    @Resource
    private BuildInfoService buildInfoService;

    @Resource
    private NginxConfService nginxConfService;

    @Override
    public void build(String name) throws DalException, IOException, SAXException {
        int ticket = buildInfoService.getTicket(name);
        nginxConfService.build(name, ticket);
    }
}