package com.allinone.web.controller.common;

import com.allinone.common.approval.*;
import com.allinone.common.core.controller.BaseController;
import com.allinone.common.core.domain.AjaxResult;
import com.allinone.common.core.page.TableDataInfo;
import com.allinone.common.exception.ServiceException;
import com.allinone.common.utils.SecurityUtils;
import com.allinone.framework.approval.*;
import com.allinone.framework.approval.mapper.ReviewMapper;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;

@RestController
@RequestMapping("/business/reviews")
public class BusinessReviewController extends BaseController {
    @Autowired private ReviewService service;
    @Autowired private ReviewMapper mapper;
    @Autowired private ApprovalIdentityService identity;
    public static class Action { public int revision; public String key; public String comment; public boolean approved; }
    @PreAuthorize("isAuthenticated()") @GetMapping
    public TableDataInfo list(@RequestParam String type){service.permission(type,"query");startPage();return getDataTable(service.list(type));}
    @PreAuthorize("isAuthenticated()") @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id){ReviewCase c=service.get(id);return success(Map.of("request",c,"events",mapper.events(id),"files",mapper.files(id)));}
    @PreAuthorize("isAuthenticated()") @PostMapping
    public AjaxResult save(@RequestBody ReviewCase request){return success(service.save(request));}
    @PreAuthorize("isAuthenticated()") @PostMapping("/{id}/submit")
    public AjaxResult submit(@PathVariable Long id,@RequestBody Action a){return success(service.submit(id,a.revision,a.key));}
    @PreAuthorize("isAuthenticated()") @PostMapping("/{id}/decide")
    public AjaxResult decide(@PathVariable Long id,@RequestBody Action a){return success(service.decide(id,a.revision,a.approved,a.comment,a.key));}
    @PreAuthorize("isAuthenticated()") @PostMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id,@RequestBody Action a){return success(service.cancel(id,a.revision,a.comment,a.key));}
    @PreAuthorize("@ss.hasPermi('system:delegation:add')") @GetMapping("/agents")
    public AjaxResult agents(){return success(mapper.users());}
    @PreAuthorize("@ss.hasPermi('system:delegation:query')") @GetMapping("/my-grants")
    public AjaxResult grants(){return success(identity.activeGrants());}
    @PreAuthorize("@ss.hasPermi('system:delegation:add')") @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id,@RequestBody Action a){service.revokeDelegation(id,a.revision,a.comment,a.key);return success();}
    @PreAuthorize("@ss.hasPermi('system:delegation:manage')") @GetMapping("/configuration")
    public AjaxResult configs(){return success(Map.of("configs",mapper.configs(),"users",mapper.users()));}
    @PreAuthorize("@ss.hasPermi('system:delegation:manage')") @PostMapping("/configuration")
    public AjaxResult config(@RequestBody ReviewConfig c){
        if(c.type==null||c.deptId==null||c.supervisorId==null)throw new ServiceException("业务、部门和审核人不能为空");
        if(c.targetKey==null)c.targetKey=0L;
        if(!Set.of("SUPPLIER_CHANGE","COLLECT_CORRECTION","DELEGATION").contains(c.type))throw new ServiceException("不支持的配置类型");
        if(!SecurityUtils.isAdmin()){
            ApprovalUser u=mapper.user(SecurityUtils.getUserId());
            if(u==null||!identity.supervisor(u.id)||(!"EXEC".equals(u.rank)&&!Objects.equals(u.deptId,c.deptId)))throw new ServiceException("无权配置该部门");
        }
        if(!identity.eligible(c.supervisorId,c.type,"SUPERVISOR"))throw new ServiceException("审核人缺少角色或审批权限");
        if(c.financeId!=null&&(!identity.eligible(c.financeId,c.type,"FINANCE")||c.financeId.equals(c.supervisorId)))throw new ServiceException("请配置独立且有资格的财务复核人");
        if("COLLECT_CORRECTION".equals(c.type)&&(c.targetKey<=0||c.openFrom==null||c.openUntil==null||!c.openUntil.after(c.openFrom)))throw new ServiceException("填报更正必须指定模板及开放期间");
        mapper.saveConfig(c);return success();
    }
    @PreAuthorize("isAuthenticated()") @PostMapping("/{id}/files")
    public AjaxResult upload(@PathVariable Long id,@RequestParam MultipartFile file) throws Exception {
        ReviewCase c=service.get(id);if(!c.canEdit)throw new ServiceException("当前申请不能上传材料");
        if(file.isEmpty()||file.getSize()>10*1024*1024)throw new ServiceException("材料必须小于10MB");
        String name=Path.of(Objects.requireNonNullElse(file.getOriginalFilename(),"材料")).getFileName().toString();
        if(name.length()>200||!name.toLowerCase(Locale.ROOT).matches(".*\\.(pdf|png|jpg|jpeg)$"))throw new ServiceException("仅支持PDF、PNG、JPG材料");
        Path root=Path.of(System.getProperty("user.dir"),"runtime","review-private").toAbsolutePath().normalize();Files.createDirectories(root);
        Path path=root.resolve(UUID.randomUUID()+".bin");byte[] bytes=file.getBytes();Files.write(path,bytes,StandardOpenOption.CREATE_NEW);
        try {service.attach(id,c.revision,name,path.toString(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));}
        catch(Exception e){Files.deleteIfExists(path);throw e;}
        return success(mapper.files(id));
    }
    @PreAuthorize("isAuthenticated()") @GetMapping("/{id}/files/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable Long id,@PathVariable Long fileId)throws Exception {
        service.get(id);Map<String,Object> f=mapper.file(id,fileId);if(f==null)throw new ServiceException("材料不存在");
        Path root=Path.of(System.getProperty("user.dir"),"runtime","review-private").toAbsolutePath().normalize();Path path=Path.of(String.valueOf(f.get("path"))).toAbsolutePath().normalize();
        if(!path.startsWith(root))throw new ServiceException("材料路径无效");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(String.valueOf(f.get("name")),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(Files.readAllBytes(path));
    }
}
