function module( name ) {
	var parts = name.split(".");
	var obj = window;
	for( i in parts ) {
		var nuobj = obj[parts[i]];
		if( nuobj == null ) {
			nuobj = obj[parts[i]] = {};
		}
		obj = nuobj;
	}
	return obj;
}
