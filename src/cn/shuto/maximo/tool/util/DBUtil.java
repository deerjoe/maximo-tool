package cn.shuto.maximo.tool.util;

import java.io.IOException;
import java.io.Writer;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;



public class DBUtil {
	private static Logger _log = Logger.getLogger(DBUtil.class.getName());
	private Connection conn = null;
	
	public void updateClob(String updateSql,String clobString){
		try {
			// 写入操作
			PreparedStatement pstmt = conn.prepareStatement(updateSql);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				// 造型为oracle.sql.CLOB
				Clob clob = rs.getClob(1);
				Writer writer = clob.setCharacterStream(1);
				writer.write(clobString);
				writer.close();
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public Connection getMaximoConnection(String maximoPath) {
		if(conn!=null){
			return conn;
		}
		try {
			_log.info("-----------打开新的数据库连接---------------");
			Properties config = PropertyReader.getProperties(maximoPath + "\\properties\\maximo.properties");
			Class.forName(config.getProperty("mxe.db.driver"));
			_log.info("--连接如下数据库--");
			_log.info("--mxe.db.url="+config.getProperty("mxe.db.url"));
			_log.info("--mxe.db.user="+config.getProperty("mxe.db.user"));
			_log.info("--mxe.db.password="+config.getProperty("mxe.db.password"));
			conn = DriverManager.getConnection(config.getProperty("mxe.db.url"),
					config.getProperty("mxe.db.user"), config.getProperty("mxe.db.password"));
			conn.setAutoCommit(false);
			return conn;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void closeConnection() {
		try {
			_log.info("-----------关闭数据库连接---------------");
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static class SingletonHolder {
		private static DBUtil instance = new DBUtil();
	}

	private DBUtil() {
	}

	public static DBUtil getInstance() {
		return SingletonHolder.instance;
	}
}
