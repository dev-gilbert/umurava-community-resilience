package rw.umurava.resource;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rw.umurava.community.CommunityMembershipRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ResourceService {
 private static final String ACTIVE="ACTIVE";
 private static final Set<String> CATEGORIES=Set.of("ENERGY","WATER","FOOD","HEALTH","TRANSPORT","SHELTER","OTHER");
 private final JdbcTemplate jdbc; private final CommunityMembershipRepository memberships;
 public ResourceService(JdbcTemplate jdbc,CommunityMembershipRepository memberships){this.jdbc=jdbc;this.memberships=memberships;}
 public record ResourceView(UUID id,UUID communityId,String name,String category,String description,BigDecimal quantity,BigDecimal minimumQuantity,String unit,String status,long version){}
 public record CreateResource(String name,String category,String description,BigDecimal quantity,BigDecimal minimumQuantity,String unit){}
 public record QuantityResult(ResourceView resource,boolean replayed){}
 @Transactional(readOnly=true) public List<ResourceView> list(UUID actor,UUID community,String category){requireMember(actor,community);if(category!=null&&!category.isBlank()){String c=category.toUpperCase();if(!CATEGORIES.contains(c))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported category");return jdbc.query(select()+" WHERE community_id=? AND category=? ORDER BY name",(rs,n)->map(rs),community,c);}return jdbc.query(select()+" WHERE community_id=? ORDER BY category,name",(rs,n)->map(rs),community);}
 @Transactional public ResourceView create(UUID actor,UUID community,CreateResource in){return create(actor,community,UUID.randomUUID(),in);}
 @Transactional public ResourceView create(UUID actor,UUID community,UUID id,CreateResource in){requireManager(actor,community);String category=in.category().toUpperCase();if(!CATEGORIES.contains(category))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported category");if(in.quantity().signum()<0||in.minimumQuantity().signum()<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantities cannot be negative");String status=in.quantity().signum()==0?"DEPLETED":"AVAILABLE";jdbc.update("INSERT INTO resources(id,community_id,name,category,description,quantity,minimum_quantity,unit,status,version) VALUES (?,?,?,?,?,?,?,?,?,1)",id,community,in.name().trim(),category,in.description(),in.quantity(),in.minimumQuantity(),in.unit().trim(),status);return find(community,id,false);}
 @Transactional public QuantityResult adjust(UUID actor,UUID community,UUID resource,String direction,BigDecimal quantity,String key){requireManager(actor,community);if(quantity==null||quantity.signum()<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be positive");if(key==null||key.isBlank()||key.length()>120)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Valid idempotency key required");String type=direction==null?"":direction.toUpperCase();if(!Set.of("INCREASE","DECREASE").contains(type))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Direction must be INCREASE or DECREASE");var old=jdbc.query("SELECT transaction_type,quantity FROM resource_transactions WHERE idempotency_key=? AND resource_id=? AND community_id=?",(rs,n)->new Object[]{rs.getString(1),rs.getBigDecimal(2)},key,resource,community);if(!old.isEmpty()){Object[] prior=old.getFirst();if(!type.equals(prior[0])||quantity.compareTo((BigDecimal)prior[1])!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key reused with different data");return new QuantityResult(find(community,resource,false),true);}ResourceView current=find(community,resource,true);BigDecimal next=type.equals("INCREASE")?current.quantity().add(quantity):current.quantity().subtract(quantity);if(next.signum()<0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Insufficient recorded quantity");jdbc.update("UPDATE resources SET quantity=?,status=?,version=version+1,updated_at=NOW() WHERE id=? AND community_id=?",next,next.signum()==0?"DEPLETED":"AVAILABLE",resource,community);jdbc.update("INSERT INTO resource_transactions(id,resource_id,community_id,transaction_type,quantity,actor_user_id,idempotency_key) VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),resource,community,type,quantity,actor,key);return new QuantityResult(find(community,resource,false),false);}
 private ResourceView find(UUID community,UUID id,boolean lock){return jdbc.query(select()+" WHERE community_id=? AND id=?"+(lock?" FOR UPDATE":""),(rs,n)->map(rs),community,id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Resource not found"));}
 private String select(){return "SELECT id,community_id,name,category,description,quantity,minimum_quantity,unit,status,version FROM resources";}
 private ResourceView map(ResultSet r)throws SQLException{return new ResourceView(r.getObject("id",UUID.class),r.getObject("community_id",UUID.class),r.getString("name"),r.getString("category"),r.getString("description"),r.getBigDecimal("quantity"),r.getBigDecimal("minimum_quantity"),r.getString("unit"),r.getString("status"),r.getLong("version"));}
 private void requireMember(UUID user,UUID community){if(!memberships.existsByUserIdAndCommunityIdAndStatus(user,community,ACTIVE))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Active community membership required");}
 private void requireManager(UUID user,UUID community){boolean ok=memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"COMMUNITY_ADMIN",ACTIVE)||memberships.existsByUserIdAndCommunityIdAndRoleAndStatus(user,community,"RESOURCE_MANAGER",ACTIVE);if(!ok)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Resource manager or community administrator required");}
}
