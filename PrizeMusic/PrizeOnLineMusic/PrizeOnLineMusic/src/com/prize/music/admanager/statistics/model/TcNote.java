package com.prize.music.admanager.statistics.model;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

import java.io.Serializable;


/**
 * 存入数据的对象
 */
@Table(name = "TcNote")
public class TcNote implements Serializable{
    @Column(name = "id",autoGen = true, isId = true)
    public int id;
    @Column(name = "firstCloumn")
    public String firstCloumn;
    @Column(name = "secondCloumn")
    public String secondCloumn;
    @Column(name = "timeStamp")
    public long timeStamp;


}
