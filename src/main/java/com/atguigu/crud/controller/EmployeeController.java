package com.atguigu.crud.controller;


import com.atguigu.crud.bean.Employee;

import com.atguigu.crud.bean.Msg;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.atguigu.crud.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处理员工CRUD请求
 */
@Controller
public class EmployeeController{

    @Autowired
    EmployeeService employeeService;

    /**
     * 单个批量二合一
     * 批量删除：1-2-3
     * 单个删除：1
     *
     * @param ids
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/emp/{ids}",method = RequestMethod.DELETE)
    public Msg deleteEmp(@PathVariable("ids")String ids){
        //批量删除
        if (ids.contains("-")){
            List<Integer> del_ids = new ArrayList<>();
            String[] str_ids = ids.split("-");
            //组装id的集合
             for (String string:str_ids){
                del_ids.add(Integer.parseInt(string));
             }
            employeeService.deleteBatch(del_ids);
        }else {
            Integer id = Integer.parseInt(ids);
            employeeService.deleteEmp(id);
        }

        return Msg.success();
    }

    /**
     * 如果直接发送ajax=PUT形式的请求
     * 封装的数据
     * 除了empId有数据剩下全是null
     *
     * 问题：
     * 请求体中有数据：
     * 但是Employee对象封装不上
     *
     * 原因：
     * Tomcat：
     *          1.将请求体中的数据，封装一个map
     *          2.request.getParameter("empName")就会从这个map中取值
     *          3.SpringMVC封装POJO对象的时候
     *                     会把POJO中每个属性的值，request.getParameter()
     *
     * AJAX发送PUT请求导致：
     *         PUT请求，请求体中的数据，request.getParameter("empName")拿不到
     *         Tomcat一看是PUT不会封装请求体中的数据为map，只有POST形式的请求封装请求体为map
     *
     * 解决方案：
     * 我们要能支持直接发送PUT之类的请求还要封装请求体中的数据
     * 配置上HttpPutFormContentFilter;
     * 它的作用：将请求体中的数据解析包装成一个map。
     * request被重新包装，request.getParameter()被重写，就会从自己封装的map中取数据
     * 员工修改数据方法
     * @param employee
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/emp/{empId}",method = RequestMethod.PUT)
    public Msg saveEmp(Employee employee){

        employeeService.updateEmp(employee);

        return Msg.success();

    }
    /**
     * 根据id查询员工
     * @param id
     * @return
     */
    @RequestMapping(value = "/emp/{id}",method = RequestMethod.GET)
    @ResponseBody
    public Msg getEmp(@PathVariable("id") Integer id){

        Employee employee = employeeService.getEmp(id);
        return Msg.success().add("emp",employee);
    }

    /**
     * 检查用户名是否可用
     * @param empName
     * @return
     */
    @RequestMapping("/checkuser")
    @ResponseBody
    public Msg checkuser(@RequestParam(value = "empName") String empName){
        //判断用户名是否是合法的表达式：
        String regx ="(^[a-z0-9_-]{6,16}$)|(^[\\u2E80-\\u9FFF]{2,5})";
        if (!empName.matches(regx)){
            return Msg.fail().add("va_msg","用户名是2-5位中文或者6-16位英文和数字的组合");
        }
        //数据库用户名重复效验
        boolean b = employeeService.checkUser(empName);
        if (b){
            return Msg.success();
        }else {
            return Msg.fail().add("va_msg","用户名不可用");
        }
    }


    /**
     * 定义员工保存
     * 1.支持JSR303效验
     * 2.导入Hibernate-Validator
     * @param employee
     * @return
     */
    @RequestMapping(value = "/emp",method = RequestMethod.POST)
    @ResponseBody
    public Msg saveEmp(@Valid Employee employee, BindingResult result){
        if(result.hasErrors()){
            //效验失败，应该返回失败，在模态框中显示效验失败的错误信息
            Map<String,Object> map = new HashMap<>();
            List<FieldError> errors = result.getFieldErrors();
            for (FieldError fieldError : errors){
                System.out.println("错误的字段名："+fieldError.getField());
                System.out.println("错误信息："+fieldError.getDefaultMessage());
                map.put(fieldError.getField(),fieldError.getDefaultMessage());
            }
            return Msg.fail().add("errorFields",map);
        }else{
            employeeService.saveEmp(employee);
            return Msg.success();
        }

    }

    /**
     * @ResponseBody需要导入jackson包，负责将对象转换成jason字符串
     * @param pn
     * @param model
     * @return
     */

    @RequestMapping("/emps")
    @ResponseBody
    public Msg getEmpsWithJson(@RequestParam(value = "pn",defaultValue = "1") Integer pn,
                               Model model){
        //这不是一个分页查询
        //引入PageHelper分页插件
        //在查询之前只需要调用,传入页码，以及每页的大小
        PageHelper.startPage(pn,5);
        //startPage后面紧跟的这个查询就是一个分页查询
        List<Employee> emps = employeeService.getAll();
        //使用pageInfo包装查询后的结果,只需要将pageInfo交给页面就行。
        //封装了详细的分页信息，包括有我们查询出来的信息,传入连续显示的页数
        PageInfo page = new PageInfo(emps,5);
        return Msg.success().add("pageInfo",page);

    }

    /**
     * 查询员工数据（分页查询）
     * @return
     */
//    @RequestMapping("/emps")
//    public String getEmps(@RequestParam(value = "pn",defaultValue = "1") Integer pn,
//                          Model model){
//        //这不是一个分页查询
//        //引入PageHelper分页插件
//        //在查询之前只需要调用,传入页码，以及每页的大小
//        PageHelper.startPage(pn,5);
//        //startPage后面紧跟的这个查询就是一个分页查询
//        List<Employee> emps = employeeService.getAll();
//        //使用pageInfo包装查询后的结果,只需要将pageInfo交给页面就行。
//        //封装了详细的分页信息，包括有我们查询出来的信息,传入连续显示的页数
//        PageInfo page = new PageInfo(emps,5);
//        model.addAttribute("pageInfo",page);
//
//
//        return "list";
//    }

}
