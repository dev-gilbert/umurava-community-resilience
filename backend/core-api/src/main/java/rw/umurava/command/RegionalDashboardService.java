package rw.umurava.command;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.UUID;

@Service
public class RegionalDashboardService {
 private final JdbcTemplate jdbc; public RegionalDashboardService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Transactional(readOnly=true) public Map<String,Object> district(UUID user,String district){if(district==null||district.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"District is required");grant(user,"DISTRICT",district);return aggregate("WHERE lower(c.district_name)=lower(?)",district);}
 @Transactional(readOnly=true) public Map<String,Object> national(UUID user){grant(user,"NATIONAL","RW");return aggregate("",null);}
 private Map<String,Object> aggregate(String filter,Object value){String communityWhere=filter.isEmpty()?"":" "+filter;String sql="SELECT (SELECT COUNT(*) FROM communities c"+communityWhere+") AS communities, (SELECT COUNT(*) FROM emergency_incidents i JOIN communities c ON c.id=i.community_id"+communityWhere.replace("c.district_name","c.district_name")+" AND i.status NOT IN ('RESOLVED','CLOSED')) AS active_incidents, (SELECT COUNT(*) FROM resources r JOIN communities c ON c.id=r.community_id"+communityWhere+") AS resources, (SELECT COALESCE(SUM(r.quantity),0) FROM resources r JOIN communities c ON c.id=r.community_id"+communityWhere+") AS resource_quantity, (SELECT COUNT(*) FROM food_batches f JOIN communities c ON c.id=f.community_id"+communityWhere+") AS food_batches, (SELECT COUNT(*) FROM water_sources w JOIN communities c ON c.id=w.community_id"+communityWhere+") AS water_sources, (SELECT COUNT(*) FROM energy_assets e JOIN communities c ON c.id=e.community_id"+communityWhere+") AS energy_assets, (SELECT COUNT(*) FROM health_facilities h JOIN communities c ON c.id=h.community_id"+communityWhere+") AS health_facilities, (SELECT COUNT(*) FROM transport_vehicles v JOIN communities c ON c.id=v.community_id"+communityWhere+") AS transport_vehicles, (SELECT COUNT(*) FROM transport_requests t JOIN communities c ON c.id=t.community_id WHERE t.status NOT IN ('COMPLETED','CANCELLED')"+(filter.isEmpty()?"":" AND lower(c.district_name)=lower(?)")+") AS open_transport_requests";if(value==null)return jdbc.queryForMap(sql);return jdbc.queryForMap(sql,value,value,value,value,value,value,value,value,value,value);}
 private void grant(UUID user,String type,String value){Boolean allowed=jdbc.query("SELECT EXISTS(SELECT 1 FROM regional_access_grants WHERE user_id=? AND scope_type=? AND lower(scope_value)=lower(?))",rs->rs.next()&&rs.getBoolean(1),user,type,value);if(!Boolean.TRUE.equals(allowed))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"A provisioned "+type.toLowerCase()+" dashboard grant is required");}
}
