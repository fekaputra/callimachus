// dropzone.js
/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
   Licensed under the Apache License, Version 2.0, http://www.apache.org/licenses/LICENSE-2.0
*/

(function($, jQuery){

$(document).ready(function() {
    initDropArea($("[data-construct]"));
});

$(document).bind('DOMNodeInserted', function (event) {
    initDropArea($(event.target).find("[data-construct]").andSelf().filter("[data-construct]"));
});

function initDropArea(construct) {
    var dropzone = construct.add(construct.parents()).filter('[dropzone]');
    dropzone.bind('dragenter dragover', function(event) {
        if (!$(this).hasClass("drag-over")) {
            $(this).addClass("drag-over");
        }
        event.preventDefault();
        return false;
    });
    dropzone.bind('dragleave', function(event) {
        $(this).removeClass("drag-over");
        event.preventDefault();
        return false;
    });
    dropzone.bind('drop', function(event) {
        $(this).removeClass("drag-over");
        event.preventDefault();
        if (typeof this.ondrop == 'function') {
            return false; // event registered
        } else if (typeof $(this).attr('ondrop') == 'string') {
            return eval('(function(){' + $(this).attr('ondrop') + '})()');
        } else {
            return calli.insertResource(event);
        }
    });
    dropzone.bind('calliLink', function(event) {
        var de = jQuery.Event('drop');
        de.dataTransfer = {getData:function(){return event.location}};
        de.errorMessage = "Invalid Relationship";
        $(event.target).trigger(de);
    });
}

})(jQuery, jQuery);

