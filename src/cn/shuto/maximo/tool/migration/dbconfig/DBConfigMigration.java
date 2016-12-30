package cn.shuto.maximo.tool.migration.dbconfig;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import cn.shuto.maximo.tool.migration.dbconfig.bean.Autokey;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxAttributeCfg;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxObjectCfg;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxRelationship;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxSequence;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxSysIndexes;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxSysKey;
import cn.shuto.maximo.tool.migration.dbconfig.bean.MaxTableCfg;
import cn.shuto.maximo.tool.util.DBUtil;
import cn.shuto.maximo.tool.util.SerializeUtil;

/**
 * 数据库配置迁移
 * 
 * @author shixw
 *
 */
public class DBConfigMigration {
	private static Logger _log = Logger.getLogger(DBConfigMigration.class.getName());
	private String MAXIMOPATH = null;
	private String PACKAGEPATH = null;
	private String DBCONFIGFILEPATH = "\\package\\dbconfig\\BDConfig.mtep";

	private Connection conn = null;

	private static final String SELECTMAXOBJECTCFG = "select OBJECTNAME, CLASSNAME, DESCRIPTION, EAUDITENABLED, EAUDITFILTER, ENTITYNAME, ESIGFILTER, EXTENDSOBJECT, IMPORTED, ISVIEW, PERSISTENT, SERVICENAME, SITEORGTYPE, USERDEFINED, CHANGED, MAINOBJECT, INTERNAL, MAXOBJECTID, TEXTDIRECTION from maxobjectcfg where objectname = ?";
	private static final String SELECTMAXTABLECFG = "select TABLENAME, ADDROWSTAMP, EAUDITTBNAME, ISAUDITTABLE, RESTOREDATA, STORAGEPARTITION, TEXTSEARCHENABLED, LANGTABLENAME, LANGCOLUMNNAME, UNIQUECOLUMNNAME, ISLANGTABLE, MAXTABLEID, ALTIXNAME, TRIGROOT, CONTENTATTRIBUTE from maxtablecfg where  tablename = ?";
	private static final String SELECTMAXATTRIBUTECFG = "select OBJECTNAME, ATTRIBUTENAME, ALIAS, AUTOKEYNAME, ATTRIBUTENO, CANAUTONUM, CLASSNAME, COLUMNNAME, DEFAULTVALUE, DOMAINID, EAUDITENABLED, ENTITYNAME, ESIGENABLED, ISLDOWNER, ISPOSITIVE, LENGTH, MAXTYPE, MUSTBE, REQUIRED, PERSISTENT, PRIMARYKEYCOLSEQ, REMARKS, SAMEASATTRIBUTE, SAMEASOBJECT, SCALE, TITLE, USERDEFINED, CHANGED, SEARCHTYPE, MLSUPPORTED, MLINUSE, HANDLECOLUMNNAME, MAXATTRIBUTEID, RESTRICTED, LOCALIZABLE, TEXTDIRECTION, COMPLEXEXPRESSION from maxattributecfg where objectname = ?";
	private static final String SELECTMAXSYSINDEXES = "select  NAME, TBNAME, UNIQUERULE, CHANGED, CLUSTERRULE, STORAGEPARTITION, REQUIRED, TEXTSEARCH, MAXSYSINDEXESID from maxsysindexes where tbname = ?";
	private static final String SELECTMAXSYSKEYS = "select IXNAME, COLNAME, COLSEQ, ORDERING, CHANGED, MAXSYSKEYSID from maxsyskeys  where IXNAME = ?";
	private static final String SELECTMAXRELATIONSHIP = "select NAME, PARENT, CHILD, WHERECLAUSE, REMARKS, MAXRELATIONSHIPID, CARDINALITY, DBJOINREQUIRED from maxrelationship where parent = ?";
	private static final String SELECTMAXSEQUENCE = "select TBNAME, NAME, MAXRESERVED, MAXVALUE, RANGE, SEQUENCENAME, MAXSEQUENCEID from maxsequence where tbname = ?";
	private static final String SELECTAUTOKEY = "select PREFIX, SEED, ORGID, SITEID, AUTOKEYNAME, SETID, LANGCODE, AUTOKEYID from autokey where AUTOKEYNAME = ?";

	private static final String INSERTMAXOBJECTCFG = "insert into maxobjectcfg ( OBJECTNAME, CLASSNAME, DESCRIPTION, EAUDITENABLED, EAUDITFILTER, ENTITYNAME, ESIGFILTER, EXTENDSOBJECT, IMPORTED, ISVIEW, PERSISTENT, SERVICENAME, SITEORGTYPE, USERDEFINED, CHANGED, MAINOBJECT, INTERNAL, MAXOBJECTID, TEXTDIRECTION) values ( '%s', '%s', '%s', %s , '%s', '%s', '%s', '%s', %s , %s , %s , '%s', '%s', %s ,'I', %s , %s , MAXOBJECTCFGSEQ.nextval, '%s');";
	PreparedStatement maxobjectcfgST = null;
	PreparedStatement maxtablecfgST = null;
	PreparedStatement maxattributecfgST = null;
	PreparedStatement maxsysindexesST = null;
	PreparedStatement maxsyskeysST = null;
	PreparedStatement maxrelationshipST = null;
	PreparedStatement maxsequenceST = null;
	PreparedStatement autokeyST = null;
	Statement importDBConfigST = null;

	public DBConfigMigration(String maximoPath, String packagePath) {
		this.MAXIMOPATH = maximoPath;
		this.PACKAGEPATH = packagePath;
		conn = DBUtil.getInstance().getMaximoConnection(MAXIMOPATH);
		if (conn != null) {
			try {
				maxobjectcfgST = conn.prepareStatement(SELECTMAXOBJECTCFG);
				maxtablecfgST = conn.prepareStatement(SELECTMAXTABLECFG);
				maxattributecfgST = conn.prepareStatement(SELECTMAXATTRIBUTECFG);
				maxsysindexesST = conn.prepareStatement(SELECTMAXSYSINDEXES);
				maxsyskeysST = conn.prepareStatement(SELECTMAXSYSKEYS);
				maxrelationshipST = conn.prepareStatement(SELECTMAXRELATIONSHIP);
				maxsequenceST = conn.prepareStatement(SELECTMAXSEQUENCE);
				autokeyST = conn.prepareStatement(SELECTAUTOKEY);

				importDBConfigST = conn.createStatement();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void importDBConfig() {
		// 反序列化数据 路径下的 数据
		List<MaxObjectCfg> list = SerializeUtil.readObjectForList(new File(PACKAGEPATH + DBCONFIGFILEPATH));
		try {
			// 遍历 maxobjectcfg 集合
			for (MaxObjectCfg maxObjectCfg : list) {
				_log.info("--插入maxobjectcfg--" + maxObjectCfg.toInsertSql());
				importDBConfigST.addBatch(maxObjectCfg.toInsertSql());
				if (maxObjectCfg.getMaxTableCfg() != null) {
					// 关联的 maxtablecfg
					_log.info("--插入maxtablecfg--" + maxObjectCfg.getMaxTableCfg().toInsertSql());
					importDBConfigST.addBatch(maxObjectCfg.getMaxTableCfg().toInsertSql());
				}
				// 关联的 maxAttributeCfg
				_log.info("--插入maxAttributeCfg---------");
				List<MaxAttributeCfg> maxAttributeCfgs = maxObjectCfg.getMaxAttributeCfgs();
				for (MaxAttributeCfg maxAttributeCfg : maxAttributeCfgs) {
					_log.info("-----插入：" + maxAttributeCfg.toInsertSql());
					importDBConfigST.addBatch(maxAttributeCfg.toInsertSql());

					_log.info("--插入autokey---------");
					List<Autokey> autokeys = maxAttributeCfg.getAutokeys();
					if (autokeys != null && autokeys.size() > 0)
						for (Autokey autokey : autokeys) {
							_log.info("--插入：" + autokey.toInsertSql());
							importDBConfigST.addBatch(autokey.toInsertSql());
						}

				}
				_log.info("--插入 MaxSysIndexes---------");
				List<MaxSysIndexes> maxSysIndexes = maxObjectCfg.getMaxSysIndexes();
				for (MaxSysIndexes maxSysIndex : maxSysIndexes) {
					_log.info("--插入：" + maxSysIndex.toInsertSql());
					importDBConfigST.addBatch(maxSysIndex.toInsertSql());
					_log.info("--插入 maxSysKeys---------");
					List<MaxSysKey> maxSysKeys = maxSysIndex.getMaxSysKeys();
					for (MaxSysKey maxSysKey : maxSysKeys) {
						_log.info("--插入：" + maxSysKey.toInsertSql());
						importDBConfigST.addBatch(maxSysKey.toInsertSql());
					}
				}
				_log.info("--插入 MaxRelationship---------");
				List<MaxRelationship> maxRelationships = maxObjectCfg.getMaxRelationships();
				if (maxRelationships != null && maxRelationships.size() > 0)
					for (MaxRelationship maxRelationship : maxRelationships) {
						_log.info("--插入：" + maxRelationship.toInsertSql());
						importDBConfigST.addBatch(maxRelationship.toInsertSql());
					}
				_log.info("--插入 maxSequences---------");
				List<MaxSequence> maxSequences = maxObjectCfg.getMaxSequences();
				if (maxSequences != null && maxSequences.size() > 0)
					for (MaxSequence maxSequence : maxSequences) {
						_log.info("--插入：" + maxSequence.toInsertSql());
						importDBConfigST.addBatch(maxSequence.toInsertSql());
					}

			}
			importDBConfigST.executeBatch();
			conn.commit();
		} catch (SQLException e) {
			try {
				// 事务回滚
				conn.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	public void exportDBConfig(String exportObjects) {
		// 需要导出的对象数组
		exportDBConfig(buildExportObjects(exportObjects));
	}

	/**
	 * 导出数据库配置
	 * 
	 * @param exportObjects
	 *            导出数据库配置的多个对象，以逗号分开
	 */
	public void exportDBConfig(String[] exportObjects) {
		_log.info("---------- 需要导出的数据库配置的对象为:" + exportObjects);
		// 存储所有导出对象的集合
		List<MaxObjectCfg> list = new ArrayList<MaxObjectCfg>();
		// 遍历所有需要迁移的对象

		try {
			for (String object : exportObjects) {
				list.add(exportObject(object));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeResource();
		}
		// 将导出的集合进行Java序列化
		SerializeUtil.writeObject(list, new File(PACKAGEPATH + DBCONFIGFILEPATH));
	}

	/**
	 * 根据对象名称导出数据库配置的对象 需要导出的表结构包括: maxobjectcfg maxtablecfg maxattributecfg
	 * maxsysindexes maxsyskeys maxrelationship maxsequence autokey
	 * 
	 * @param objectName
	 * @throws SQLException
	 */
	private MaxObjectCfg exportObject(String objectName) throws SQLException {
		_log.info("导出对象：" + objectName);
		// 导出maxobjectcfg表
		MaxObjectCfg maxobjectcfg = exportMaxobjectcfgToJavaBean(objectName);
		// 判断是否为视图
		if (maxobjectcfg.getIsview() == 1) {// 是视图

		} else {// 不是视图
				// 导出Maxtablecfg表
			_log.info("导出对象：" + objectName + " 对应的Maxtablecfg表中的数据-");
			MaxTableCfg maxTableCfg = exportMaxtablecfgToJavaBean(maxobjectcfg.getEntityname());
			maxobjectcfg.setMaxTableCfg(maxTableCfg);
		}

		// 导出 MaxAttributeCfg
		_log.info("导出对象：" + objectName + " 对应的Maxattributecfg表中的数据-");
		maxobjectcfg.setMaxAttributeCfgs(exportMaxattributecfgToJavaBean(objectName));

		// 导出 MaxAttributeCfg
		_log.info("导出对象：" + objectName + " 对应的maxsysindexes表中的数据-");
		maxobjectcfg.setMaxSysIndexes(exportMaxsysindexesToJavaBean(objectName));

		// 导出 MaxRelationship
		_log.info("导出对象：" + objectName + " 对应的MaxRelationship表中的数据-");
		maxobjectcfg.setMaxRelationships(exportMaxrelationshipToJavaBean(objectName));

		// 导出 maxsequence
		_log.info("导出对象：" + objectName + " 对应的maxsequence表中的数据-");
		maxobjectcfg.setMaxSequences(exportMaxsequenceToJavaBean(maxobjectcfg.getEntityname()));
		return maxobjectcfg;
	}

	/**
	 * 导出 Maxsequence 表数据
	 * 
	 * @param entityName
	 * @return
	 * @throws SQLException
	 */
	public List<MaxSequence> exportMaxsequenceToJavaBean(String entityName) throws SQLException {
		List<MaxSequence> list = new ArrayList<MaxSequence>();
		maxsequenceST.setString(1, entityName);
		ResultSet rs = maxsequenceST.executeQuery();
		while (rs.next()) {
			list.add(new MaxSequence(NULLTOEMPTY(rs.getString(1)), NULLTOEMPTY(rs.getString(2)), rs.getInt(3),
					rs.getInt(4), rs.getInt(5), NULLTOEMPTY(rs.getString(6))));
		}
		rs.close();
		return list;
	}

	/**
	 * 导出 MaxRelationship 表数据
	 * 
	 * @param objectName
	 * @return
	 * @throws SQLException
	 */
	public List<MaxRelationship> exportMaxrelationshipToJavaBean(String objectName) throws SQLException {
		List<MaxRelationship> list = new ArrayList<MaxRelationship>();
		maxrelationshipST.setString(1, objectName);
		ResultSet rs = maxrelationshipST.executeQuery();
		while (rs.next()) {
			list.add(new MaxRelationship(NULLTOEMPTY(rs.getString(1)), NULLTOEMPTY(rs.getString(2)),
					NULLTOEMPTY(rs.getString(3)), NULLTOEMPTY(rs.getString(4)), NULLTOEMPTY(rs.getString(5)),
					NULLTOEMPTY(rs.getString(7)), rs.getInt(8)));
		}
		rs.close();
		return list;
	}

	/**
	 * 导出 Maxsysindexes 数据
	 * 
	 * @param objectName
	 * @return
	 * @throws SQLException
	 */
	public List<MaxSysIndexes> exportMaxsysindexesToJavaBean(String objectName) throws SQLException {
		maxsysindexesST.setString(1, objectName);
		List<MaxSysIndexes> maxSysIndexes = new ArrayList<MaxSysIndexes>();
		ResultSet rs = maxsysindexesST.executeQuery();
		while (rs.next()) {
			String name = NULLTOEMPTY(rs.getString(1));
			_log.info("--导出索引名字为：" + name + " 的数据----------------");
			MaxSysIndexes maxSysIndex = new MaxSysIndexes(name, NULLTOEMPTY(rs.getString(2)),
					NULLTOEMPTY(rs.getString(3)), rs.getInt(5), NULLTOEMPTY(rs.getString(6)), rs.getInt(7),
					rs.getInt(8));

			_log.info("--导出索引名字为：" + name + " maxsyskeys 表的数据----------------");
			maxSysIndex.setMaxSysKeys(exportMaxsyskeysToJavaBean(name));
			// 将索引对象添加的 集合中
			maxSysIndexes.add(maxSysIndex);
		}
		return maxSysIndexes;
	}

	/**
	 * 导出 索引表 对应的 maxsyskeys 表中的数据
	 * 
	 * @param indexName
	 * @return
	 * @throws SQLException
	 */
	public List<MaxSysKey> exportMaxsyskeysToJavaBean(String indexName) throws SQLException {
		List<MaxSysKey> list = new ArrayList<MaxSysKey>();
		maxsyskeysST.setString(1, indexName);
		ResultSet rs = maxsyskeysST.executeQuery();
		while (rs.next()) {
			list.add(new MaxSysKey(NULLTOEMPTY(rs.getString(1)), NULLTOEMPTY(rs.getString(2)), rs.getInt(3),
					NULLTOEMPTY(rs.getString(4))));
		}
		rs.close();
		return list;
	}

	/**
	 * 导出 MaxAttributeCfg 中的数据
	 * 
	 * @param objectName
	 * @return
	 */
	public List<MaxAttributeCfg> exportMaxattributecfgToJavaBean(String objectName) throws SQLException {
		maxattributecfgST.setString(1, objectName);
		ResultSet rs = maxattributecfgST.executeQuery();
		List<MaxAttributeCfg> list = new ArrayList<MaxAttributeCfg>();
		while (rs.next()) {
			String attributename = NULLTOEMPTY(rs.getString(2));
			String autokeyName = rs.getString(4);
			_log.info("--导出属性：" + attributename + "----------------");
			MaxAttributeCfg maxAttributeCfg = new MaxAttributeCfg(NULLTOEMPTY(rs.getString(1)), attributename,
					NULLTOEMPTY(rs.getString(3)), NULLTOEMPTY(autokeyName), rs.getInt(5), rs.getInt(6),
					NULLTOEMPTY(rs.getString(7)), NULLTOEMPTY(rs.getString(8)), NULLTOEMPTY(rs.getString(9)),
					NULLTOEMPTY(rs.getString(10)), rs.getInt(11), NULLTOEMPTY(rs.getString(12)), rs.getInt(13),
					rs.getInt(14), rs.getInt(15), rs.getInt(16), NULLTOEMPTY(rs.getString(17)), rs.getInt(18),
					rs.getInt(19), rs.getInt(20), rs.getInt(21), NULLTOEMPTY(rs.getString(22)),
					NULLTOEMPTY(rs.getString(23)), NULLTOEMPTY(rs.getString(24)), rs.getInt(25),
					NULLTOEMPTY(rs.getString(26)), rs.getInt(27), NULLTOEMPTY(rs.getString(29)), rs.getInt(30),
					rs.getInt(31), NULLTOEMPTY(rs.getString(32)), rs.getInt(34), rs.getInt(35),
					NULLTOEMPTY(rs.getString(36)), NULLTOEMPTY(rs.getString(37)));

			if (autokeyName != null) {
				_log.info("--导出属性：" + attributename + "---对应的Autokey,名字为：" + autokeyName + "-------------");
				maxAttributeCfg.setAutokeys(exportAutokeyToJavaBean(autokeyName));
			}

			list.add(maxAttributeCfg);
		}
		rs.close();
		return list;
	}

	/**
	 * 导出 autokey 表数据
	 * 
	 * @param autokeyName
	 * @return
	 * @throws SQLException
	 */
	public List<Autokey> exportAutokeyToJavaBean(String autokeyName) throws SQLException {
		List<Autokey> list = new ArrayList<Autokey>();
		autokeyST.setString(1, autokeyName);
		ResultSet rs = autokeyST.executeQuery();
		while (rs.next()) {
			list.add(new Autokey(NULLTOEMPTY(rs.getString(1)), rs.getInt(2), NULLTOEMPTY(rs.getString(3)),
					NULLTOEMPTY(rs.getString(4)), NULLTOEMPTY(rs.getString(5)), NULLTOEMPTY(rs.getString(6)),
					NULLTOEMPTY(rs.getString(7))));
		}
		rs.close();
		return list;
	}

	/**
	 * 导出Maxtablecfg表中的数据
	 * 
	 * @param objectName
	 * @return
	 * @throws SQLException
	 */
	public MaxTableCfg exportMaxtablecfgToJavaBean(String entityName) throws SQLException {
		maxtablecfgST.setString(1, entityName);
		ResultSet rs = maxtablecfgST.executeQuery();
		if (rs.next()) {
			return new MaxTableCfg(NULLTOEMPTY(rs.getString(1)), rs.getInt(2), NULLTOEMPTY(rs.getString(3)),
					rs.getInt(4), rs.getInt(5), NULLTOEMPTY(rs.getString(6)), rs.getInt(7),
					NULLTOEMPTY(rs.getString(8)), NULLTOEMPTY(rs.getString(9)), NULLTOEMPTY(rs.getString(10)),
					rs.getInt(11), NULLTOEMPTY(rs.getString(13)), NULLTOEMPTY(rs.getString(14)),
					NULLTOEMPTY(rs.getString(15)));
		}
		rs.close();
		return null;
	}

	/**
	 * 导出对象为JavaBean
	 * 
	 * @param objectName
	 * @return
	 * @throws SQLException
	 */
	public MaxObjectCfg exportMaxobjectcfgToJavaBean(String objectName) throws SQLException {

		maxobjectcfgST.setString(1, objectName);
		ResultSet rs = maxobjectcfgST.executeQuery();
		if (rs.next()) {
			return new MaxObjectCfg(NULLTOEMPTY(rs.getString(1)), NULLTOEMPTY(rs.getString(2)),
					NULLTOEMPTY(rs.getString(3)), rs.getInt(4), NULLTOEMPTY(rs.getString(5)),
					NULLTOEMPTY(rs.getString(6)), NULLTOEMPTY(rs.getString(7)), NULLTOEMPTY(rs.getString(8)),
					rs.getInt(9), rs.getInt(10), rs.getInt(11), NULLTOEMPTY(rs.getString(12)),
					NULLTOEMPTY(rs.getString(13)), rs.getInt(14), NULLTOEMPTY(rs.getString(16)),
					NULLTOEMPTY(rs.getString(17)), NULLTOEMPTY(rs.getString(19)));
		}
		rs.close();
		return null;
	}

	public String exportMaxobjectcfg(String objectName) throws SQLException {

		maxobjectcfgST.setString(1, objectName);
		ResultSet rs = maxobjectcfgST.executeQuery();
		if (rs.next()) {
			return String.format(INSERTMAXOBJECTCFG, NULLTOEMPTY(rs.getString(1)), NULLTOEMPTY(rs.getString(2)),
					NULLTOEMPTY(rs.getString(3)), rs.getInt(4), NULLTOEMPTY(rs.getString(5)),
					NULLTOEMPTY(rs.getString(6)), NULLTOEMPTY(rs.getString(7)), NULLTOEMPTY(rs.getString(8)),
					rs.getInt(9), rs.getInt(10), rs.getInt(11), NULLTOEMPTY(rs.getString(12)),
					NULLTOEMPTY(rs.getString(13)), rs.getInt(14), NULLTOEMPTY(rs.getString(16)),
					NULLTOEMPTY(rs.getString(17)), NULLTOEMPTY(rs.getString(19)));
		}
		rs.close();
		return null;
	}

	/**
	 * 判断字段是否为空 如果空 返回 "" 不能返回 "null"
	 * 
	 * @param v
	 * @return
	 */
	private String NULLTOEMPTY(String v) {
		return v == null ? "" : v;
	}

	/**
	 * 关闭打开的资源
	 */
	private void closeResource() {
		_log.info("----------关闭打开的资源-----------------------");
		try {
			if (maxobjectcfgST != null)
				maxobjectcfgST.close();
			if (maxtablecfgST != null)
				maxtablecfgST.close();
			if (maxattributecfgST != null)
				maxattributecfgST.close();
			if (maxsysindexesST != null)
				maxsysindexesST.close();
			if (maxsyskeysST != null)
				maxsyskeysST.close();
			if (maxrelationshipST != null)
				maxrelationshipST.close();
			if (maxsequenceST != null)
				maxsequenceST.close();
			if (autokeyST != null)
				autokeyST.close();
			if (importDBConfigST != null)
				importDBConfigST.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 以 “,” 为分隔符构建需要导出的对象数组
	 * 
	 * @param exportObjects
	 * @return
	 */
	private String[] buildExportObjects(String exportObjects) {
		return exportObjects.split(",");
	}

}
