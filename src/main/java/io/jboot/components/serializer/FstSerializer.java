/**
 * Copyright (c) 2015-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jboot.components.serializer;

import com.jfinal.log.Log;
import org.nustaq.serialization.FSTConfiguration;


public class FstSerializer implements JbootSerializer {


    private static final Log LOG = Log.getLog(FstSerializer.class);
    private static FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();

    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        return fst.asByteArray(obj);
    }

    @Override
    public Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return fst.asObject(bytes);
        } catch (Exception ex) {
            LOG.error("FstSerializer deserialize error!");
        }
        return null;
    }


}
