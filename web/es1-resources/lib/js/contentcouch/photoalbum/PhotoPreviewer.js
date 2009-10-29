var pa = module("contentcouch.photoalbum");
pa.PhotoPreviewer = function() {
	this.previews = [];
	this.currentPreviewIndex = 0;
}
var ppp = pa.PhotoPreviewer.prototype;

var Preview = function( smallUrl, mediumUrl, largeUrl ) {
	this.smallUrl = smallUrl;
	this.mediumUrl = mediumUrl;
	this.largeUrl = largeUrl;
}

ppp.addPreview = function( smallUrl, mediumUrl, largeUrl ) {
	var index = this.previews.length;
	this.previews.push( new Preview(smallUrl, mediumUrl, largeUrl ) );
	return index;
}
ppp.showPreview = function( index ) {
	if( index != null ) {
		this.currentPreviewIndex = index|0;
		//alert( "Index "+index );
		this.previewContainer.style.display = "block";
		this.previewContainer.style.position = "fixed";
		//this.previewContainer.style.width = ((window.innerWidth|0) - 96) + "px";
		//this.previewContainer.style.height = ((window.innerHeight|0) - 64) + "px";
		//this.previewContainer.style.top = (((window.innerHeight|0)-500)/2)+"px";
		//this.previewContainer.style.left = "16px";
		var preview = this.previews[index];
		this.previewLink.href = preview.largeUrl;
		this.previewImage.src = this.loadingImageUrl;
		this.previewImage.src = preview.mediumUrl;
	
		var previousPreview = (index == 0) ? null : this.previews[index-1];
		this.previousLinkBox.style.display = (previousPreview == null) ? "none" : "block";
		if( previousPreview != null ) {
			this.previousThumbnail.src = previousPreview.smallUrl;
		}
	
		var nextPreview = (index == this.previews.length-1) ? null : this.previews[index+1];
		this.nextLinkBox.style.display = (nextPreview == null) ? "none" : "block";
		if( nextPreview != null ) {
			this.nextThumbnail.src = nextPreview.smallUrl;
		}
	} else {
		this.previewContainer.style.display = "none";
	}
}
ppp.hidePreview = function() {
	this.showPreview( null );
}
ppp.showPreviewBasedOnUrl = function() {
	var match = /#preview(\d+)/.exec(window.location.href);
	if( match ) {
		var index = match[1];
		this.showPreview( index|0 );
	} else {
		this.hidePreview();
	}
}
ppp.goToPreview = function( index ) {
	this.showPreview( index );
	return false;
}
ppp.goToPreviousPreview = function() {
	return this.goToPreview(this.currentPreviewIndex-1);
}
ppp.goToNextPreview = function() {
	return this.goToPreview(this.currentPreviewIndex+1);
}
