package eu.arrowhead.core.datamanager.database.service;

import java.util.Optional;
//import java.util.Set;
import java.util.List;
import java.util.ArrayList; 
import java.util.Iterator; 
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.CommonConstants;
import eu.arrowhead.common.CoreCommonConstants;
import eu.arrowhead.common.Utilities;
//import eu.arrowhead.common.database.entity.System;
import eu.arrowhead.common.database.repository.SystemRepository;
import eu.arrowhead.common.dto.internal.DTOConverter;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.dto.shared.SenML;
import eu.arrowhead.common.exception.InvalidParameterException;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


@Service
public class DataManagerDBService {
	//=================================================================================================
	// members
	
	private Connection connection = null;

	@Value("${spring.datasource.url}")
	private String url;
	@Value("${spring.datasource.username}")
	private String user;
	@Value("${spring.datasource.password}")
	private String password;

	private static final Logger logger = LogManager.getLogger(DataManagerDBService.class);
	
        @Autowired
        private SystemRepository systemRepository;

	@Value(CommonConstants.$SERVER_SSL_ENABLED_WD)
	private boolean secure;

	
	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	private Connection getConnection() throws SQLException {
	  return DriverManager.getConnection(url, user, password);
	}


	//-------------------------------------------------------------------------------------------------
	private void closeConnection(Connection conn) throws SQLException {
	  conn.close();
	}

	//-------------------------------------------------------------------------------------------------
	private int serviceToID(String systemName, String serviceName, Connection conn) {
	  int id=-1;

	  PreparedStatement stmt;
	  try {
	    String sql = "SELECT id FROM dmhist_services WHERE system_name=? AND service_name=? LIMIT 1;";
	    stmt = conn.prepareStatement(sql);
	    stmt.setString(1, systemName);
	    stmt.setString(2, serviceName);
	    ResultSet rs = stmt.executeQuery();

	    rs.next();
	    id  = rs.getInt("id");

	    rs.close();
	    stmt.close();
	  } catch(SQLException se){
	    id = -1;
	  } catch(Exception e){
	    id = -1;
	  }

	  return id;
	}

	//-------------------------------------------------------------------------------------------------
	public ArrayList<String> getAllHistorianSystems() {
	  ArrayList<String> ret = new ArrayList<String>();
	  Connection conn = null;

	  try {
	    conn = getConnection();
	    String sql = "SELECT DISTINCT(system_name) FROM dmhist_services;";
	    PreparedStatement stmt = conn.prepareStatement(sql);

	    ResultSet rs = stmt.executeQuery();
	    while(rs.next() == true) {
	      ret.add(rs.getString(1));
	    }
	    rs.close();
	    stmt.close();
	  } catch (SQLException e) {
	    logger.debug(e.toString());
	  } finally {
	      try {
	        closeConnection(conn);
	      } catch (SQLException e) {
	        logger.debug(e.toString());
	      }
	  }

	  return ret;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean addServiceForSystem(String systemName, String serviceName, String serviceType) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(systemName, serviceName, conn);
	    if (id != -1) {
	      return false; //already exists
	    } else {
	      String sql = "INSERT INTO dmhist_services(system_name, service_name, service_type) VALUES(?, ?, ?);";
	      PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	      stmt.setString (1, systemName);
	      stmt.setString (2, serviceName);
	      stmt.setString (3, serviceType);
	      stmt.executeUpdate();
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      id = rs.getInt(1);
	      rs.close();
	      stmt.close();

	    }

	  } catch (SQLException e) {
	    return false;
	  } finally {
	    try {
	      closeConnection(conn);
	    } catch (SQLException e) {}

	  }

	  return true;
	}

	//-------------------------------------------------------------------------------------------------
	public ArrayList<String> getServicesFromSystem(String systemName) {
	  ArrayList<String> ret = new ArrayList<String>();
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    String sql = "SELECT DISTINCT(service_name) FROM dmhist_services WHERE system_name=?;";
	    PreparedStatement stmt = conn.prepareStatement(sql);
	    stmt.setString(1, systemName);

	    ResultSet rs = stmt.executeQuery();
	    while(rs.next() == true) {
	      ret.add(rs.getString(1));
	    }
	    rs.close();
	    stmt.close();
	  } catch(SQLException db){
	    logger.error(db.toString());
	  }

	  finally {
	    try {
	      closeConnection(conn);
	    }catch(SQLException db){
	      logger.error(db.toString());
	    }

	  }

	  return ret;
	}


	//-------------------------------------------------------------------------------------------------
	public boolean createEndpoint(String systemName, String serviceName) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(systemName, serviceName, conn);
	    if (id != -1) {
	      return true; //already exists
	    } else {
	      String sql = "INSERT INTO dmhist_services(system_name, service_name) "+
		"VALUES(?,?);";
	      PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	      stmt.setString (1, systemName);
	      stmt.setString (2, serviceName);
	      stmt.executeUpdate();
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      id = rs.getInt(1);
	      rs.close();
	      stmt.close();

	    }

	  } catch (SQLException db) {
	    logger.error(db.toString());
	    return false;
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){
	      logger.error(e.toString());
	    }

	  }

	  return true;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean updateEndpoint(String systemName, String serviceName, Vector<SenML> msg) {
	  boolean ret = true;

	  double bt = msg.get(0).getBt();
	  double maxTs = getLargestTimestamp(msg);
	  double minTs = getSmallestTimestamp(msg);

	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int sid = serviceToID(systemName, serviceName, conn);
	    if (sid != -1) {
	      String sql = "INSERT INTO dmhist_messages(sid, bt, mint, maxt, msg) VALUES(?, ?, ?, ?, ?)";
	      //System.out.println("sql=" + sql);
	      PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	      stmt.setLong(1, sid);
	      stmt.setDouble(2, bt);
	      stmt.setDouble(3, minTs);
	      stmt.setDouble(4, maxTs);
	      stmt.setString(5, msg.toString());

	      int mid = stmt.executeUpdate();
	      ResultSet rs = stmt.getGeneratedKeys();
	      rs.next();
	      mid = rs.getInt(1);
	      rs.close();
	      stmt.close();

	      // that was the entire message, now insert each individual JSON object in the message
	      String bu = msg.get(0).getBu();
	      for (SenML m : msg) {
		double t = 0;
		if (m.getT() != null) {
		  if (m.getT() < 268435456) { //if relative ts, update it
		    t = m.getT() + bt;
		  }
		} else {
		  t = bt;
		}

		if (m.getU() == null) {
		  m.setU(bu);
		}

		if (m.getN() != null) {
		  sql = "INSERT INTO dmhist_entries(sid, mid, n, t, u, v, vs, vb) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
		  stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		  stmt.setInt(1, sid);
		  stmt.setInt(2, mid);
		  stmt.setString(3, m.getN());
		  stmt.setDouble(4, t);
		  stmt.setString(5, m.getU());
		  if (m.getV() == null) {
		    stmt.setNull(6, java.sql.Types.DOUBLE);
		  } else {
		    stmt.setDouble(6, m.getV());
		  } 
		  stmt.setString(7, m.getVs());
		  if (m.getVb() != null) {
		    stmt.setBoolean(8, m.getVb());
		  } else {
		    stmt.setNull(8, java.sql.Types.BOOLEAN);
		  }
		  stmt.executeUpdate();
		  rs = stmt.getGeneratedKeys();
		  rs.close();
		  stmt.close();
		}

	      }

	    } else {
	      ret = false;
	    }
	  } catch (SQLException e) {
	    ret = false;
	    //System.out.println("Exception: " + e.toString() + "\n" + e.getStackTrace());
	  } finally {
	    try{
	      closeConnection(conn);
	    } catch(Exception e){
	    }

	  }

	  return ret;
	}

	//-------------------------------------------------------------------------------------------------
	public Vector<SenML> fetchEndpoint(String systemName, String serviceName, double from, double to, int count, Vector<String> signals) {
	  Connection conn = null;
	  try {
	    conn = getConnection();
	    int id = serviceToID(systemName, serviceName, conn);
	    if (id == -1)
	      return null;

	    String signalss = "";
	    if (signals != null) {
	      signalss = String.join(", ", signals);
	      signalss = signalss.substring(0, signalss.length()-1); 
	    }

	    if (from == -1) {
	      from = 0;                                       //1970-01-01
	    }
	    if (to == -1) {
	      to = 1000 + (long)(System.currentTimeMillis() / 1000.0); // current timestamp - not ok to insert data that we created in the future
	    }

	    String sql = "";
	    PreparedStatement stmt = conn.prepareStatement(sql);
	    if (signals != null) {
	      sql = "SELECT * FROM dmhist_entries WHERE sid=? AND n IN (?) AND t>=? AND t<=? ORDER BY t DESC;";
	      stmt.setInt(1, id);
	      stmt.setString(2, signalss);
	      stmt.setDouble(3, from);
	      stmt.setDouble(4, to);
	    } else {
	      sql = "SELECT * FROM dmhist_entries WHERE sid=? AND t>=? AND t <=? ORDER BY t DESC;";
	      stmt.setString(1, systemName);
	      stmt.setInt(1, id);
	      stmt.setDouble(2, from);
	      stmt.setDouble(3, to);
	    }

	    ResultSet rs = stmt.executeQuery();

	    Vector<SenML> messages = new Vector<SenML>();
	    SenML hdr = new SenML();
	    hdr.setBn(serviceName);
	    messages.add(hdr);
	    double bt = 0;
	    String bu = null;
	    while(rs.next() == true && count > 0) {
	      SenML msg = new SenML();
	      msg.setT((double)rs.getLong("t"));
	      msg.setN(rs.getString("n"));
	      msg.setU(rs.getString("u"));
	      final double v = rs.getDouble("v");
	      if (!rs.wasNull()) {
		msg.setV(v);
	      }

	      msg.setVs(rs.getString("vs"));
	      Boolean foo = rs.getBoolean("vb");
	      if (!rs.wasNull())
		msg.setVb(rs.getBoolean("vb"));

	      messages.add(msg);
	      count--;
	    }

	    rs.close();
	    stmt.close();

	    if (messages.size() == 1)
	      return messages;

	    //recalculate a bt time and update all relative timestamps
	    double startbt = ((SenML)messages.get(1)).getT();
	    ((SenML)messages.firstElement()).setBt(startbt);
	    ((SenML)messages.firstElement()).setT(null);
	    ((SenML)messages.get(1)).setT(null);
	    for (SenML m : messages) {
	      if (m.getT() != null) {
		m.setT(m.getT()-startbt);
	      }
	    }

	    return messages;

	  } catch (SQLException e) {
	    logger.debug(e.toString());
	  } finally {
	    try {
	      closeConnection(conn);
	    } catch(Exception e){
	    }

	  }

	  return null;
	}


	//=================================================================================================
	// assistant methods
  
	//-------------------------------------------------------------------------------------------------
	//returns largest (newest) timestamp value
	private double getLargestTimestamp(Vector<SenML> msg) {
	  double bt = msg.get(0).getBt();
	  double max = bt;
	  for (SenML m : msg) {

	    if (m.getT() == null) {
	      continue;
	    }
	    if (m.getT() > 268435456) { // absolute
	      if (m.getT() > max ) {
		max = m.getT();
	      }
	    } else {                      //relative
	      if (m.getT()+bt > max ) {
		max = m.getT() + bt;
	      }
	    }
	  }

	  return max;
	}

	//-------------------------------------------------------------------------------------------------
	//returns smallest (oldest) timestamp value
	private double getSmallestTimestamp(Vector<SenML> msg) {
	  double bt = msg.get(0).getBt();
	  double min = bt;
	  for (SenML m : msg) {

	    if (m.getT() == null) {
	      continue;
	    }
	    if (m.getT() > 268435456) { // absolute
	      if (m.getT() < min ) {
		min = m.getT();
	      }
	    } else {                      //relative
	      if (m.getT()+bt < min ) {
		min = m.getT() + bt;
	      }
	    }
	  }

	  return min;
	}

}
