package xyz.codevomit.demostreamer.rest;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import xyz.codevomit.demostreamer.common.BattlefieldConstants;
import xyz.codevomit.demostreamer.exception.UsernameAlreadyInUseException;

/**
 *
 * @author merka
 */
@Controller
@Slf4j
public class HomePageController
{
    @Value("${server.contextPath:}")
    String serverContextPath;
    
    @Autowired
    UserDetailsManager userManager;
    
    @RequestMapping(value = {"", "/"})
    public String land(Model model)
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth != null && !(auth.getName().equals(BattlefieldConstants.ANONYMOUS_USER))){
            model.addAttribute("user", auth.getPrincipal());
        }
        return "index";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public RedirectView join(@RequestParam(name = "username", required = true) String username)
    {
        log.debug("Join request received with username = {}", username);
        if(StringUtils.isBlank(username)){
            throw new IllegalArgumentException("username cannot be blank");
        }
        if(userManager.userExists(username)){
            throw new UsernameAlreadyInUseException(username);
        }
        
        String trimmedUsername = username.trim();
        List<GrantedAuthority> authorities = Arrays.asList(new SimpleGrantedAuthority(BattlefieldConstants.PLAYER_ROLE));
        UserDetails user = new User(trimmedUsername, "", authorities);
        userManager.createUser(user);
        
        authenticateUser(user);
        
        String url = redirectUrl("/battlefield.html");
        return new RedirectView(url);
    }
    
    @RequestMapping(path = "/custom-logout", method = RequestMethod.POST)
    public RedirectView logout(Model model){
        SecurityContextHolder.clearContext();
        if(model.containsAttribute("user")){
            model.addAttribute("user", null);
        }
        return new RedirectView(redirectUrl("/"));
    }
    
    public String redirectUrl(String relativeUrl){
        return serverContextPath + relativeUrl;
    }
    
    public void authenticateUser(UserDetails user){
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(user, user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    
    @ExceptionHandler(value = UsernameAlreadyInUseException.class)
    public String usernameAlreadyInUse(Model model)
    {
        model.addAttribute("errorMessage", "Sorry, username already in use");
        return "index";
    }
}
