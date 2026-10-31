package uniquecore;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class HelloController {
    
    @GetMapping("/hello")
    public String hello() {
        return "gov-doc-ai 서버 정상 동작";
    }
    
}
