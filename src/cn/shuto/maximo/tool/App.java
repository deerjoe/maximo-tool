package cn.shuto.maximo.tool;

import java.util.logging.Logger;

import cn.shuto.maximo.tool.migration.dbconfig.DBConfigMigration;
import cn.shuto.maximo.tool.migration.domainadm.DomainadmMigration;
import cn.shuto.maximo.tool.system.SystemEnvironmental;
import cn.shuto.maximo.tool.util.DBUtil;

/**
 * Hello world!
 *
 */
public class App {
	private static Logger _log = Logger.getLogger(App.class.getName());

	public static void main(String[] argv) {
		// 注册系统退出事件，退出系统时关闭数据库连接
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				_log.info("------------系统退出,释放资源!--------------");
				DBUtil.getInstance().closeConnection();
			}
		});

		SystemEnvironmental systemEnvironmental = SystemEnvironmental.getInstance();
		// 解析参数
		for (int i = 0; i < argv.length; i++) {
			String[] paramArray = argv[i].split("=");
			systemEnvironmental.putParam(paramArray[0], paramArray[1]);
		}
		//获取操作
		String option = systemEnvironmental.getStringParam("-option");
		if (option != null && !"".equals(option)) {
			if ("exportdbconfig".equals(option)) {
				DBConfigMigration dbcm = new DBConfigMigration();
				dbcm.exportDBConfig(systemEnvironmental.getStringParam("-exportobjects"));
			}
			if ("importdbconfig".equals(option)) {
				DBConfigMigration dbcm = new DBConfigMigration();
				dbcm.importDBConfig();
			}
			if("exportdomainadm".equals(option)){
				DomainadmMigration dm = new DomainadmMigration();
				dm.exportDomainadm(systemEnvironmental.getStringParam("-exportdomainids"));
			}
			if ("importdomainadm".equals(option)) {
				DomainadmMigration dm = new DomainadmMigration();
				dm.importDomainadm();
			}
		}

	}
}
