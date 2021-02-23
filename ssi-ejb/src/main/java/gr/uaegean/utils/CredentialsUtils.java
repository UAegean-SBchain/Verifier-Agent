/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uaegean.utils;

import gr.uaegean.pojo.VerifiableCredential;
import gr.uaegean.pojo.VerifiableCredential.VerifiedClaims;
import gr.uaegean.services.EthereumService;
import gr.uaegean.services.impl.EthereumServiceImpl;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author nikos
 */
@Slf4j
public class CredentialsUtils {

    private static EthereumService ETH_SERV = new EthereumServiceImpl();

    public static VerifiedClaims getClaimsFromVerifiedArray(VerifiableCredential credential) {
        boolean result = ETH_SERV.checkRevocationStatus(getCredentialId(credential));
        log.info("Revocation List result {} !!!!!!!!", result);
        return credential.getVerified()[0].getClaims();
    }

    private static String getCredentialId(VerifiableCredential cred){
        VerifiedClaims vc = cred.getVerified()[0].getClaims();

        if(vc.getId() != null){
            return vc.getId();
        }else{
            if(vc.getTaxisId()!= null){
                return vc.getTaxisId().getMetadata().getId();
            }
            if(vc.getCivilRegistryId() != null){
                return vc.getCivilRegistryId().getMetadata().getId();
            }
            if(vc.getEBillId() != null){
                return vc.getEBillId().getMetadata().getId();
            }
            if(vc.getFStatus() != null){
                return vc.getFStatus().getMetadata().getId();
            }
            if(vc.getContact() != null){
                return vc.getContact().getMetadata().getId();
            }

            if(vc.getDeclaration() != null){
                return vc.getDeclaration().getMetadata().getId();
            }


        }

        return "1";
    }


}
