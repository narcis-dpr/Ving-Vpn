/******************************************************************************
 *                                                                            *
 * Copyright (C) 2021 by nekohasekai <contact-sagernet@sekai.icu>             *
 *                                                                            *
 * This program is free software: you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation, either version 3 of the License, or          *
 *  (at your option) any later version.                                       *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              *
 * GNU General Public License for more details.                               *
 *                                                                            *
 * You should have received a copy of the GNU General Public License          *
 * along with this program. If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                            *
 ******************************************************************************/

package io.nekohasekai.sagernet.ftm.naive;

import androidx.annotation.NonNull;

import io.nekohasekai.sagernet.ftm.AbstractBean;
import io.nekohasekai.sagernet.ftm.KryoConverters;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;


public class NaiveBean extends AbstractBean {

    /**
     * Available proto: https, quic.
     */
    public String proto;
    public String username;
    public String password;
    public String extraHeaders;
    public String sni;
    public String certificates;
    public Integer insecureConcurrency;

    @Override
    public void initializeDefaultValues() {
        if (serverPort == null) serverPort = 443;
        super.initializeDefaultValues();
        if (proto == null) proto = "https";
        if (username == null) username = "";
        if (password == null) password = "";
        if (extraHeaders == null) extraHeaders = "";
        if (certificates == null) certificates = "";
        if (sni == null) sni = "";
        if (insecureConcurrency == null) insecureConcurrency = 0;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(2);
        super.serialize(output);
        output.writeString(proto);
        output.writeString(username);
        output.writeString(password);
        // note: sequence is different from SagerNet,,,
        output.writeString(extraHeaders);
        output.writeString(certificates);
        output.writeString(sni);
        output.writeInt(insecureConcurrency);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        proto = input.readString();
        username = input.readString();
        password = input.readString();
        extraHeaders = input.readString();
        if (version >= 2) {
            certificates = input.readString();
            sni = input.readString();
        }
        if (version >= 1) {
            insecureConcurrency = input.readInt();
        }
    }

    @NotNull
    @Override
    public NaiveBean clone() {
        return KryoConverters.deserialize(new NaiveBean(), KryoConverters.serialize(this));
    }

    public static final Creator<NaiveBean> CREATOR = new CREATOR<NaiveBean>() {
        @NonNull
        @Override
        public NaiveBean newInstance() {
            return new NaiveBean();
        }

        @Override
        public NaiveBean[] newArray(int size) {
            return new NaiveBean[size];
        }
    };
}
