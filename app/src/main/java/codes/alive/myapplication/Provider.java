package codes.alive.myapplication;

import android.nfc.tech.IsoDep;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;

import java.io.IOException;

public class Provider implements IProvider {

	private IsoDep mTagCom;

	public Provider(IsoDep mTagCom) {
		this.mTagCom = mTagCom;
	}

	@Override
	public byte[] transceive(final byte[] pCommand) throws CommunicationException {
		
		byte[] response;
		try {
			// send command to emv card
			response = mTagCom.transceive(pCommand);
		} catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		}

		return response;
	}

	@Override
	public byte[] getAt() {
        // For NFC-A
		return mTagCom.getHistoricalBytes();
		// For NFC-B
        // return mTagCom.getHiLayerResponse();
	}


	public void setmTagCom(final IsoDep mTagCom) {
		this.mTagCom = mTagCom;
	}

}