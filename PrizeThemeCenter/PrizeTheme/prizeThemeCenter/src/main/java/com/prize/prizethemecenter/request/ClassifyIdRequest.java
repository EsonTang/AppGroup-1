package com.prize.prizethemecenter.request;

import com.prize.app.constants.Constants;

import org.xutils.http.annotation.HttpRequest;
import org.xutils.http.app.DefaultParamsBuilder;

/**
 * Created by pengy on 2016/9/2.
 * 首页主题请求
 */
@HttpRequest(
        host = Constants.GIS_URL,
        path = "ThemeStore/ThemeInfo/getThemesByType",
        builder = DefaultParamsBuilder.class)
public class ClassifyIdRequest extends  BaseRequest {

    /**请求分类Id*/
    public int typeId;
    /**请求页数*/
    public int pageIndex ;
    /**每次请求数量*/
    public int pageSize ;

    public ClassifyIdRequest(){

    }

}
