package top.itning.hrms.service.impl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.itning.hrms.dao.StaffDao;
import top.itning.hrms.dao.WageDao;
import top.itning.hrms.dao.department.DepartmentDao;
import top.itning.hrms.dao.department.GrassrootDao;
import top.itning.hrms.dao.job.JobTitleDao;
import top.itning.hrms.entity.ServerMessage;
import top.itning.hrms.entity.Staff;
import top.itning.hrms.entity.Wage;
import top.itning.hrms.entity.search.SearchWage;
import top.itning.hrms.exception.json.JsonException;
import top.itning.hrms.service.WageService;
import top.itning.hrms.util.ObjectUtils;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import java.util.*;

/**
 * 职工工资服务实现类
 *
 * @author Ning
 */
@Service
@Transactional(rollbackOn = Exception.class)
public class WageServiceImpl implements WageService {
    private static final Logger logger = LoggerFactory.getLogger(WageServiceImpl.class);

    @Autowired
    private WageDao wageDao;

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private StaffDao staffDao;

    @Autowired
    private GrassrootDao grassrootDao;

    @Autowired
    private JobTitleDao jobTitleDao;

    @Override
    public String[] getWageYear() {
        return wageDao.findYear();
    }

    @Override
    public Map<String, Object> searchWage(SearchWage searchWage) throws JsonException {
        logger.debug("searchWage::开始搜索职工");
        logger.info("searchWage::搜索实体信息->" + searchWage);
        //如果选了多个年并且选了年下的月
        if (searchWage.getMonth() != null && searchWage.getYear().length != 1) {
            logger.info("searchWage::开始添加选中年但未选中月");
            a:
            for (String year : searchWage.getYear()) {
                for (String month : searchWage.getMonth()) {
                    if (year.equals(month.substring(0, 4))) {
                        continue a;
                    }
                }
                String[] allMonth = new String[]{year + "-1", year + "-2", year + "-3", year + "-2", year + "-4", year + "-5", year + "-6", year + "-7", year + "-8", year + "-9", year + "-10", year + "-11", year + "-12"};
                searchWage.setMonth(ArrayUtils.addAll(searchWage.getMonth(), allMonth));
            }
            logger.info("searchWage::完成添加添加选中年但未选中月");
        }
        Map<String, Object> stringObjectHashMap = new HashMap<>(2);
        List<Wage> wageList = wageDao.findAll((root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            //查询条件:姓名(Name)
            if (StringUtils.isNoneBlank(searchWage.getName())) {
                logger.info("searchWage::查询条件 name(精确查询)->" + searchWage.getName());
                List<Predicate> predicateList = new ArrayList<>();
                staffDao.findByName(searchWage.getName()).forEach(staff -> predicateList.add(cb.equal(root.get("staff"), staff)));
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:身份证号(nid)
            if (StringUtils.isNoneBlank(searchWage.getNid())) {
                logger.info("searchWage::查询条件 nid(精确查询)->" + searchWage.getNid());
                List<Predicate> predicateList = new ArrayList<>();
                staffDao.findByNid(searchWage.getNid()).forEach(staff -> predicateList.add(cb.equal(root.get("staff"), staff)));
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:部门ID(department)
            if (searchWage.getDepartment() != null) {
                logger.info("searchWage::查询条件 department(多条件查询)->" + Arrays.toString(searchWage.getDepartment()));
                List<Predicate> predicateList = new ArrayList<>();
                for (int i = 0; i < searchWage.getDepartment().length; i++) {
                    staffDao.findByDepartment(departmentDao.getOne(searchWage.getDepartment()[i])).forEach(staff -> predicateList.add(cb.equal(root.get("staff"), staff)));
                }
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:基层单位ID(grassroot)
            if (searchWage.getGrassroot() != null) {
                logger.info("searchWage::查询条件 grassroot(多条件查询)->" + Arrays.toString(searchWage.getGrassroot()));
                List<Predicate> predicateList = new ArrayList<>();
                for (int i = 0; i < searchWage.getGrassroot().length; i++) {
                    staffDao.findByGrassroot(grassrootDao.getOne(searchWage.getGrassroot()[i])).forEach(staff -> predicateList.add(cb.equal(root.get("staff"), staff)));
                }
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:社会职称(jobTitle)
            if (searchWage.getJobTitle() != null) {
                logger.info("searchWage::查询条件 jobTitle(多条件查询)->" + Arrays.toString(searchWage.getJobTitle()));
                List<Predicate> predicateList = new ArrayList<>();
                for (int i = 0; i < searchWage.getJobTitle().length; i++) {
                    staffDao.findByJobTitle(jobTitleDao.getOne(searchWage.getJobTitle()[i])).forEach(staff -> predicateList.add(cb.equal(root.get("staff"), staff)));
                }
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:年(year)
            if (searchWage.getYear() != null) {
                logger.info("searchWage::查询条件 year(精确查询)->" + Arrays.toString(searchWage.getYear()));
                List<Predicate> predicateList = new ArrayList<>();
                for (int i = 0; i < searchWage.getYear().length; i++) {
                    predicateList.add(cb.equal(root.get("year"), searchWage.getYear()[i]));
                }
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            //查询条件:月(month)
            if (searchWage.getMonth() != null) {
                logger.info("searchWage::查询条件 month(精确查询)->" + Arrays.toString(searchWage.getMonth()));
                List<Predicate> predicateList = new ArrayList<>();
                for (int i = 0; i < searchWage.getMonth().length; i++) {
                    predicateList.add(cb.equal(root.get("month"), searchWage.getMonth()[i]));
                }
                list.add(cb.or(predicateList.toArray(new Predicate[predicateList.size()])));
            }
            Predicate[] p = new Predicate[list.size()];
            return cb.and(list.toArray(p));
        });
        stringObjectHashMap.put("wageList", wageList);
        try {
            Wage allFieldsSum = ObjectUtils.getAllFieldsSum(wageList, Wage.class);
            Staff staff = new Staff();
            staff.setWage(wageList.stream().mapToInt(w -> w.getStaff().getWage()).sum());
            staff.setPerformancePay(wageList.stream().mapToInt(w -> w.getStaff().getPerformancePay()).sum());
            staff.setDutyAllowance(wageList.stream().mapToInt(w -> {
                if (w.getStaff().getDutyAllowance() == null) {
                    return 0;
                } else {
                    return w.getStaff().getDutyAllowance();
                }
            }).sum());
            staff.setGrants(wageList.stream().mapToInt(w -> {
                if (w.getStaff().getGrants() == null) {
                    return 0;
                } else {
                    return w.getStaff().getGrants();
                }
            }).sum());
            allFieldsSum.setStaff(staff);
            stringObjectHashMap.put("sumWage", allFieldsSum);
        } catch (IllegalAccessException | InstantiationException e) {
            throw new JsonException("求和出现异常,请联系管理员->" + e.getMessage(), ServerMessage.SERVICE_ERROR);
        }
        return stringObjectHashMap;
    }
}