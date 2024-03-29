# 2021-10-30 Preface

The following format is all well and good,
and I'd been thinking about how to represent RDF as JSON
in a way that isn't totally annoying for a long time.

As it turns out, there's [JSON-LD](https://json-ld.org/) ([spec](https://www.w3.org/TR/json-ld/)),
which provides a conventional way to map JSON-encoded objects to RDF.
If I decide to make JSON representations of objects,
I should make sure the representation I choose is compatible with JSON-LD.

You can use [the JSON-LD playground](https://json-ld.org/playground/)
or [RDFLib](https://github.com/RDFLib/rdflib) (a python library)
or maybe [JSON-LD lint](https://github.com/mattrglobal/jsonld-lint) (haven't tested) to validate.


# Proposal for a simplified RDF serialization format for encoding directories and commits based on JSON

Changelog:
- 2014-12-16: Originally written
- 2021-10-30: s/classUri/classRef/
  - Though if JSON-LD, maybe just use `@type`!  And `@id` instead of `uri`.
  

## Directory

```
{
	"classRef": "http://ns.nuke24.net/ContentCouch/Directory",
	"entries": [
		{
			"name": "A Subdirectory",
			"target": {
				"classRef": "http://ns.nuke24.net/ContentCouch/Directory",
				"uri": "x-rdf-subject:urn:bitprint:QMWRICSFPENL7HRTRGC7DA5VZIREYVTG.Z5OAUTQ6R5CYY3KTVIXHELUS5RHENFMC3BLR3ZQ"
			}
		},
		{
			"name": "Josh.jpg",
			"target": {
				"classRef": "http://ns.nuke24.net/ContentCouch/Blob",
				"uri": "urn:bitprint:FQMXI25FR6BDDL4VCXH3S354FXBQFNWO.D5XJH6LABGRW7STOBRYID7KIQUUQWFHFS4BBCLY",
				"fileLength": 3109320
			},
			"modified": "2014-01-03 16:52:24 GMT"
		},
		{
			// Let's leave out "classRef" for directory entries.
			// It's implied to be "http://ns.nuke24.net/ContentCouch/DirectoryEntry".
			"name": "puppy.jpg",
			"target": {
				"classRef": "http://ns.nuke24.net/ContentCouch/Blob",
				"uri": "urn:bitprint:RMMC6UCNGQHOVMDWUYLIYZ74CAQXDYPK.XV3RIKWP66GYUN7TWWPKH2DSOYUHSXP5QDLPE4A",
				"fileLength": 315764
			}
			"modified": "2014-05-26 01:31:48 GMT",
			"comment": "OMG cute dog!"
		}
	]
}
```

## Commit

```
{
	"classRef": "http://ns.nuke24.net/ContentCouch/Commit",
	"parentUri": "http://ns.nuke24.net/ContentCouch/Directory",
	"target": {
		"classRef": "http://ns.nuke24.net/ContentCouch/Directory",
		"uri": "x-rdf-subject:urn:bitprint:65D2JF5JOFF7FYWWFZBLTUEBRTLACUPV.GJKD7NNPVK3EQ62KLE5R26EE6R4MGFKQSVNTS2Q"
	},
	"created": "2013-11-12 02:07:48 GMT",
	"creator": "maude-trtd",
	"description": "Music archive on Maude, 2013-11-11"
}
```

## Formatting specifics

Following these ensures that there is a single, 'canonical'
representation of each object, which I've tried to follow in the
examples above.

- Directory entries should sorted by their UTF-8 representation
- Each level of indentation should be a single tab.
- Files should be terminated by a newline.

## Notes

The idea here is that we store the same structure represented in the
RDF+XML documents in JSON.  Preferrably in a way that's easy to
translate back and forth, or even interpret as an RDF serialization
format.

Following is the proposed mapping.

Examples:

Reference an object by URI:

```
<someProperty rdf:resource="urn:something"/>
```

```
"somePropertyUri": "urn:something"
```

or

```
"someProperty": { "uri": "urn:something" }
```

Talk about an object:

```
<SomeClass xmlns="http://namespace/">...</SomeClass>
```

```
{
  "classRef": "http://namespace/SomeClass",
  ...
}
```

Or (even shorter, somewhat less precise), just using the last part of the class URI, calling it 'class name':

```
{
  "className": "SomeClass",
  ...
}
```

