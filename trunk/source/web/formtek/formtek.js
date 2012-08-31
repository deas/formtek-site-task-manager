/**
 * Copyright (C) 2012 Formtek, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Formtek root namespace.
 * 
 * @namespace Formtek
 */
// Ensure Formtek root object exists
if (typeof Formtek == "undefined" || !Formtek)
{
   var Formtek = {};
}

/**
 * Formtek top-level constants namespace.
 * 
 * @namespace Formtek
 * @class Formtek.constants
 */
Formtek.constants = Formtek.constants || {};

/**
 * Formtek top-level module namespace.
 * 
 * @namespace Formtek
 * @class Formtek.module
 */
Formtek.module = Formtek.module || {};

/**
 * Formtek top-level dashlet namespace.
 * 
 * @namespace Formtek
 * @class Formtek.dashlet
 */
Formtek.dashlet = Formtek.dashlet || {};