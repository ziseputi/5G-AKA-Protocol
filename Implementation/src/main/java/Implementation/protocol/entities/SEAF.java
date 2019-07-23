package Implementation.protocol.entities;

import Implementation.helper.Calculator;
import Implementation.helper.Converter;
import Implementation.helper.SHA256;
import Implementation.protocol.messages.*;
import Implementation.structure.Entity;
import Implementation.structure.Message;

public class SEAF extends Entity {

    public AUSF ausf = null;
    public UE ue = null;

    public final byte[] servingNetworkName;

    public SEAF(byte[] servingNetworkName) {
        this.servingNetworkName = servingNetworkName;
    }

    @Override
    public String getName() {
        return "SEAF";
    }

    @Override
    public void onReceiveMessage(Message message, Entity sender) {
        if (message instanceof N1_Registration_Request && sender instanceof UE) {
            //Received N1 Registration Request
            N1_Registration_Request n1 = (N1_Registration_Request) message;
            UE ue = (UE) sender;

            Nausf_UEAuthentication_Authenticate_Request authRequest = getAuthRequest(n1, ue);

            sendMessage(authRequest, this.ausf);
        } else if (message instanceof Nausf_UEAuthentication_Authenticate_Response && sender instanceof AUSF) {
            //Received Nausf_UEAuthentication_ Authenticate Response
            Nausf_UEAuthentication_Authenticate_Response authResponse = (Nausf_UEAuthentication_Authenticate_Response) message;
            AUSF ausf = (AUSF) sender;

            if (checkExpiryTimer(authResponse, ausf)) {
                Authentication_Request authRequest = getAuthRequest(authResponse, ausf);

                sendMessage(authRequest, this.ue);
            }
        } else if (message instanceof Authentication_Response && sender instanceof UE) {
            //Received Authentication Response
            Authentication_Response authResponse = (Authentication_Response) message;
            UE ue = (UE) sender;

            if (calculateHresAndCompare(authResponse, ue)) {
                //Consider authentication as successful.
                System.out.println(getName() + " is considering the authentication as successful.");
                Nausf_UEAuthentication_Confirmation_Request confirmRequest = getConfirmRequest(authResponse, ue);

                sendMessage(confirmRequest, this.ausf);
            }
        } else if (message instanceof Nausf_UEAuthentication_Confirmation_Response && sender instanceof AUSF) {
            //Received Nausf_UEAuthentication_ Authenticate Response (/Confirmation Response)
            System.out.println("Authentication was successful.");
        } else {
            String name = message == null ? null : message.getName();
            System.err.println(getName() + ": Received an unusual message: " + (name == null ? "" : name) + ". Ignoring it.");
        }
    }

    private Nausf_UEAuthentication_Authenticate_Request getAuthRequest(N1_Registration_Request n1, UE ue) {
        if (n1.SUCI != null) {
            return new Nausf_UEAuthentication_Authenticate_Request(n1.SUCI, true, this.servingNetworkName);
        } else if (n1.fiveGGUTI != null) {
            byte[] SUPI = n1.fiveGGUTI.getSUPI();
            return new Nausf_UEAuthentication_Authenticate_Request(SUPI, false, this.servingNetworkName);
        }
        return null;
    }

    /**
     * @param authResponse
     * @param ausf
     * @return true if timer is not expired
     */
    private boolean checkExpiryTimer(Nausf_UEAuthentication_Authenticate_Response authResponse, AUSF ausf) {
        //TODO
        return true;
    }

    private HXRESstar hXRESSstar = new HXRESstar();

    private Authentication_Request getAuthRequest(Nausf_UEAuthentication_Authenticate_Response authResponse, AUSF ausf) {
        hXRESSstar.HXRESstar = authResponse.seAV.HXRESstar;
        hXRESSstar.RAND = authResponse.seAV.RAND;
        //TODO: Store the HXRES* temporary.
        return new Authentication_Request(authResponse.seAV.RAND, authResponse.seAV.AUTN);
    }

    /**
     * @param authResponse
     * @param ue
     * @return true if calculated HXRES equals the previously stored one.
     */
    private boolean calculateHresAndCompare(Authentication_Response authResponse, UE ue) {
        //TODO
        //Derive HXRESstar
        //3GPP TS 33.501 V15.34.1 Page 155
        byte[] P0 = this.hXRESSstar.RAND;
        byte[] P1 = authResponse.RESstar;
        byte[] S = Converter.concatenateBytes(P0, P1);

        byte[] HRESstar = SHA256.encode(S);
        return Calculator.equals(this.hXRESSstar.HXRESstar, HRESstar);
    }

    private Nausf_UEAuthentication_Confirmation_Request getConfirmRequest(Authentication_Response authResponse, UE ue) {
        return new Nausf_UEAuthentication_Confirmation_Request(authResponse.RESstar);
    }

    private class HXRESstar {
        //TODO: Improve this.
        byte[] HXRESstar;
        byte[] RAND;
    }
}
