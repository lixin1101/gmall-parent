package loggers.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Demo2Controller {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Demo2Controller.class);
    @RequestMapping("/logaaaaaaa")
    public String dolog(@RequestParam("log") String logJson){
        JSONObject jsonObject = JSON.parseObject(logJson);
        jsonObject.put("ts",System.currentTimeMillis());
        logger.info(jsonObject.toJSONString());
        //System.out.println(logJson);
        //return logJson;
        return "sucess";
    }
}
