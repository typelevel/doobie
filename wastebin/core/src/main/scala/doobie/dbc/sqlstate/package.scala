package doobie.dbc

/** 
 * Tables of SQLState values from various vendors. Doobie makes no attempt to abstract errors in a
 * vendor-neutral way; if you wish to handle a SQLException you must trap the proper state for your
 * chosen back-end.
 */
package object sqlstate {

  object postgresql extends PostgreSqlState

}

