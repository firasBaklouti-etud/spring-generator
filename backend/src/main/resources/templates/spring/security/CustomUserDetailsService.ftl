package ${packageName}.service.auth;

import ${packageName}.repository.${repositoryName};
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ${repositoryName} repository;

    public CustomUserDetailsService(${repositoryName} repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findBy${usernameField?cap_first}(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
