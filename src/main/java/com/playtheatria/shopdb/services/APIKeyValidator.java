package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.User;
import com.playtheatria.shopdb.models.exceptions.ExceptionMessage;
import com.playtheatria.shopdb.models.exceptions.SDBUnauthorizedException;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.util.ModularCrypt;

import javax.enterprise.context.ApplicationScoped;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@ApplicationScoped
public class APIKeyValidator {
    private final PasswordFactory FACTORY;

    public APIKeyValidator() throws NoSuchAlgorithmException {
        this.FACTORY = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, new WildFlyElytronPasswordProvider());
    }

    public void validateAPIKey(String authHeader) throws SDBUnauthorizedException {
        if (authHeader == null) {
            System.out.println("Authorization header is null.");
            throw new SDBUnauthorizedException(ExceptionMessage.UNAUTHORIZED);
        }

        String token = authHeader.replace("Bearer ", "");

        User apiUser = User.find("username", System.getenv("SHOPDB_API_USERNAME")).firstResult();
        if (apiUser == null) {
            System.out.println("No API user found.");
            throw new SDBUnauthorizedException(ExceptionMessage.NO_API_USER);
        }

        try {
            if (!validate(token, apiUser.password)) {
                System.out.println("Authorization token is invalid.");
                throw new SDBUnauthorizedException(ExceptionMessage.UNAUTHORIZED);
            }
        } catch (InvalidKeySpecException | InvalidKeyException e) {
            System.out.println("Authorization key is invalid.");
            throw new SDBUnauthorizedException(ExceptionMessage.UNAUTHORIZED);
        }
    }

    private boolean validate(String a, String b) throws InvalidKeySpecException, InvalidKeyException {
        Password p = ModularCrypt.decode(b);
        BCryptPassword bcp = (BCryptPassword) FACTORY.translate(p);
        return FACTORY.verify(bcp, a.toCharArray());
    }
}
