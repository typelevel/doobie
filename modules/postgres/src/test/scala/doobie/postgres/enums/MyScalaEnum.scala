// Copyright (c) 2013-2018 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.postgres.enums

// create type myenum as enum ('foo', 'bar') <-- part of setup

object MyScalaEnum extends Enumeration { val foo, bar = Value }
