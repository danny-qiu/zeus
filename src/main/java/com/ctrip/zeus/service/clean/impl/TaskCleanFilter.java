package com.ctrip.zeus.service.clean.impl;

import com.ctrip.zeus.dal.core.TaskDao;
import com.ctrip.zeus.dal.core.TaskDo;
import com.ctrip.zeus.service.clean.CleanFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.unidal.dal.jdbc.DalException;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by fanqq on 2015/10/20.
 */
@Service("taskCleanFilter")
public class TaskCleanFilter implements CleanFilter {
    @Resource
    private TaskDao taskDao ;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void runFilter() throws Exception {
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(new Date());
        rightNow.add(Calendar.DAY_OF_YEAR,-7);
        Date dt = rightNow.getTime();
        this.taskDao.deleteBeforeDatetime(new TaskDo().setCreateTime(dt));
    }

    @Override
    public int interval() {
        return 24;
    }
}