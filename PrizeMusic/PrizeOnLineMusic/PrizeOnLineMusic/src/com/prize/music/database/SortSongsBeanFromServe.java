/*******************************************
 *版权所有©2015,深圳市铂睿智恒科技有限公司
 *
 *内容摘要：
 *当前版本：
 *作	者：
 *完成日期：
 *修改记录：
 *修改日期：
 *版 本 号：
 *修 改 人：
 *修改内容：
...
 *修改记录：
 *修改日期：
 *版 本 号：
 *修 改 人：
 *修改内容：
 *********************************************/

package com.prize.music.database;

import java.io.Serializable;
import java.util.ArrayList;

public class SortSongsBeanFromServe implements Serializable {

	/** 用一句话描述这个变量表示什么 */
	private static final long serialVersionUID = 8963236740658442373L;
	public ArrayList<MusicInfo> list;
	public int pageCount;
	public int pageIndex;
	public int pageSize;
	public int pageItemCount;

}
