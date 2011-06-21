// page-edit-wym.js

jQuery(function($){

	function outter(node) {
		var html = $($('<div/>').html($(node).clone())).html();
		$(node).remove();
		return html;
	}

	$(document).ready(function() {
		WYMeditor.XhtmlValidator._attributes['core']['attributes'].push('data-dialog');
		WYMeditor.XhtmlValidator._attributes['core']['attributes'].push(
			'rel',
			'rev',
			'content',
			'href',
			'src',
			'about',
			'property',
			'resource',
			'datatype',
			'typeof');
		$('.wym_box_0').wymeditor({
			html: '<h1></h1>',
			lang: null,
			initSkin: false,
			loadSkin: false,
			jQueryPath: '/callimachus/scripts/jquery.js',
			dialogFeatures: 'jQuery.dialog',
			dialogFeaturesPreview: 'jQuery.dialog',
			dialogImageHtml: outter($('.wym_dialog_image')),
			dialogTableHtml: outter($('.wym_dialog_table')),
			dialogPasteHtml: outter($('.wym_dialog_paste')),
			dialogPreviewHtml: outter($('.wym_dialog_preview')),
			dialogLinkHtml: outter($('.wym_dialog_link'))
		});
	});

	function select(node, selector) {
		if (!node)
			return $([]);
		var set = $(node).find(selector).andSelf();
		set = set.add($(node).parents(selector));
		return set.filter(selector);
	}

	function find(node, selector) {
		if (!node)
			return $([]);
		return $(node).find(selector).andSelf().filter(selector);
	}

	function setFocusToNode(wym, node) {
		updateBody();
		try {
			wym.setFocusToNode(node);
		} catch (e) { }
	}

	function insertContainer(html, toggle) {
		var block = $(this.findUp(this.container(), WYMeditor.BLOCKS)).get(0);
		var node = $(this.findUp(this.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
		if((!node || !node.parentNode) && block && block.parentNode) {
			var div = $(html);
			$(block).append(div);
			setFocusToNode(this, div.find('p')[0]);
		} else if(!node || !node.parentNode) {
			var div = $(html);
			$(this._doc.body).append(div);
			setFocusToNode(this, div.find('p')[0]);
		} else if (toggle && $(node.parentNode).is(toggle) && $(node.parentNode).children().length < 2) {
			$(node.parentNode).replaceWith(node);
			setFocusToNode(this, node);
		} else if (toggle && $(node.parentNode).is(toggle)) {
			$(node.parentNode).after(node);
			setFocusToNode(this, node);
		} else {
			var div = $(html);
			var clone = $(node).clone();
			div.find('p').replaceWith(clone);
			$(node).replaceWith(div);
			setFocusToNode(this, clone[0]);
		}
	}

	function insertField(html, toggle) {
		var block = $(this.findUp(this.container(), WYMeditor.BLOCKS)).get(0);
		var node = $(this.findUp(this.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
		if (toggle && select(this.container(), toggle).length) {
			var field = select(this.container(), toggle);
			var parent = field.parent();
			field.remove();
			setFocusToNode(this, parent[0]);
		} else if ((!node || !node.parentNode) && block && block.parentNode) {
			this.insert(html);
		} else if (!node || !node.parentNode) {
			var div = $(html);
			$(this._doc.body).append(div);
			setFocusToNode(this, div);
		} else {
			var div = $(html);
			$(node).before(div);
			setFocusToNode(this, node);
		}
	}

	function insertInput(dialog_input, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Input', null, dialog_input);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val(find(node, 'label').text());
			$('#curie').val($(node).html().match(/\{([^}]+)\}/)[1]);
		} else {
			$('.wym_dialog_input .wym_delete').remove();
		}
		$('.wym_dialog_input .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_input .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var label = $('#label').val();
			var curie = $('#curie').val();
			var id = find(node, '[id]').attr('id');
			if (!id) {
				id = generateUniqueId();
			}
			if (!checkCURIE(curie, this)) {
				return false;
			}
			insertTemplate(node, template, {type: type, label: label,
				id: id, curie: curie});
			closeDialogue();
			return false;
		});
	}

	function insertSelect(dialog_select, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Select', null, dialog_select);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#curie').val(find(node, '[rel]').attr('rel'));
			var scheme = find(node, '[rel=skos:inScheme]').attr('resource');
			var options = $('#scheme')[0].options;
			for (var i = 0; i < options.length; i++) {
				if (scheme == options[i].getAttribute('about')) {
					$('#scheme')[0].selectedIndex = i;
				}
			}
		} else {
			$('.wym_dialog_select .wym_delete').remove();
		}
		if ($('#scheme')[0].options.length <= 0) {
			jQuery.get('/callimachus/Page?schemes', function(xml) {
				$(xml).find('result').each(function() {
					var uri = $(this).find('[name=uri]>*').text();
					var label = $(this).find('[name=label]>*').text();
					var option = $('<option/>');
					option.attr('value', uri);
					option.text(label);
					$('#scheme').append(option);
				});
			}, 'xml');
		}
		$('.wym_dialog_select .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_select .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var label = $($('#scheme')[0].options[$('#scheme')[0].selectedIndex]).text();
			var curie = $('#curie').val();
			var scheme = $('#scheme').val();
			var id = find(node, '[id]').attr('id');
			if (!id) {
				id = generateUniqueId();
			}
			if (!scheme) {
				$(this).trigger('calliError', 'A scheme is required for this field');
			}
			if (!checkCURIE(curie, this)) {
				return false;
			}
			insertTemplate(node, template, {type: type, label: label,
				id: id, rel: attr, curie: curie,
				scheme: scheme});
			closeDialogue();
			return false;
		});
	}

	function InsertDropZone(dialog_drop, type, template) {
		var toggle = 'div.field.' + type;
		this.dialog('Insert Drop Zone', null, dialog_drop);
		var node = select(this.container(), toggle)[0];
		if (node) {
			$('#label').val($(node).find('label').text());
			$('#curie').val($(node).find('[rel]').attr('rel'));
			$('#class').val($(node).find('[typeof]').attr('typeof'));
			$('#prompt').val($(node).find('[data-dialog]').attr('data-dialog'));
		} else {
			$('.wym_dialog_dropzone .wym_delete').remove();
		}
		$('.wym_dialog_dropzone .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_dropzone .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var label = $('#label').val();
			var curie = $('#curie').val();
			var ctype = $('#class').val();
			var prompt = $('#prompt').val();
			var id = find(node, '[id]').attr('id');
			if (!id) {
				id = generateUniqueId();
			}
			if (!checkCURIE(curie, this) || !checkCURIE(ctype, this)) {
				return false;
			}
			insertTemplate(node, template, {type: type, label: label,
				id: id, rel: attr, curie: curie,
				'class': ctype, prompt: prompt});
			closeDialogue();
			return false;
		});
	}

	function insertTemplate(node, template, bindings) {
		var wym = window.WYMeditor.INSTANCES[0];
		var html = template;
		for (key in bindings) {
			html = html.replace(new RegExp('\\$' + key + '\\b', 'g'), bindings[key]);
		}
		if (html.indexOf('class="' + bindings['type'] + ' field"') < 0) {
			var label = '<label for="' + bindings['id'] + '">' + bindings['label'] + '</label>\n\t';
			html = '<div class="' + bindings['type'] + ' field">\n\t' + label + html + '\n</div>\n';
		}
		if (node && node.parentNode) {
			var div = $(html);
			$(node).replaceWith(div);
			setFocusToNode(wym, div);
		} else {
			var toggle = 'div.field.' + bindings['type'];
			insertField.call(wym, html, toggle);
		}
	}

	function insertForm(dialog_form) {
		var wym = this;
		this.dialog('Insert Form', null, dialog_form);
		var node = jQuery(this.findUp(this.container(), ['form'])).get(0);
		if (node) {
			$('#curie').val($(node).attr('typeof'));
			if ($(node).find('button[onclick]').length) {
				$('#create-radio')[0].checked = false;
				$('#edit-radio')[0].checked = true;
			} else {
				$('#edit-radio')[0].checked = false;
				$('#create-radio')[0].checked = true;
			}
		} else {
			$('.wym_dialog_form .wym_delete').remove();
		}
		$('.wym_dialog_form .wym_delete').click(function(event) {
			$(node).remove();
			closeDialogue();
		});
		$('.wym_dialog_form .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var curie = $('#curie').val();
			if (curie && !checkCURIE(curie, this)) {
				return false;
			}
			var form = $('<form about="?this" action="">\n<div class="vbox">\n<p></p></div>\n</form>\n');
			if (curie) {
				form.attr('typeof', curie);
			}
			if ($('#create-radio').is(':checked')) {
				form.append('<button type="submit" disabled="disabled">Create</button>\n');
			} else {
				form.append('<button type="submit" disabled="disabled">Save</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="location.replace(\'?view\')">Cancel</button>\n');
				form.append('<button type="button" disabled="disabled" onclick="calli.deleteResource(form)">Delete</button>\n');
			}
			insertContainer.call(wym, form, 'form');
			closeDialogue();
			return false;
		});
	}

	function insertRel(dialog_rel) {
		var wym = this;
		this.dialog('Insert Rel', null, dialog_rel);
		var blocks = new Array("address", "div", "dl", "fieldset", "form", "noscript", "ol", "ul", "dd", "dt", "li", "tr");
		var node = jQuery(this.findUp(this.container(), blocks)).get(0);
		if (node) {
			$('#curie').val($(node).attr('rel'));
		}
		$('.wym_dialog_rel .wym_submit').parents('form').submit(function(event) {
			event.preventDefault();
			var attr = 'rel';
			var curie = $('#curie').val();
			var id = find(node, '[id]').attr('id');
			if (!id) {
				id = generateUniqueId();
			}
			if (!checkCURIE(curie, this)) {
				return false;
			}
			if (node && node.parentNode) {
				$(node).removeAttr('rel');
				$(node).removeAttr('rev');
				$(node).attr(attr, curie);
				$(node).attr('resource', '?' + id);
			} else {
				node = jQuery(wym.findUp(wym.container(), WYMeditor.MAIN_CONTAINERS)).get(0);
				if (node && node.parentNode) {
					var div = $('<div/>');
					div.attr('class', 'vbox');
					div.attr(attr, curie);
					div.attr('resource', '?' + id);
					div.append($(node).clone());
					$(node).replaceWith(div);
					setFocusToNode(wym, div.children()[0]);
				} else {
					var div = '<div class="vbox" ' + attr + '="' + curie + '" resource="?' + id + '"><p>{rdfs:label}</p></div>';
					$(wym._doc.body).append(div);
					setFocusToNode(wym, div.children()[0]);
				}
			}
			closeDialogue();
			return false;
		});
	}

	function checkCURIE(curie, form) {
		if (curie && curie.match(/^\w+:\w+$/))
			return true;
		$(form).trigger('calliError', 'CURIE must have be a word prefix and word suffix separated by a colon');
		return false;
	}

	var generateUniqueIdCounter = 0;
	function generateUniqueId() {
		return 'id' + generateUniqueIdCounter++;
	}

	function closeDialogue() {
		if (window.opener) {
			window.close();
		} else if (window.dialog) {
			window.dialog.dialog('close');
		}
		var wym = window.WYMeditor.INSTANCES[0];
		wym.update();
		$(document).trigger('calliSuccess');
	}

	function formatXML(text) {
		if (window.XML) {
			if (text.search(/^<\?xml/) == 0) {
				text = text.substring(text.indexOf('?>') + 2);
			}
			return window.XML(text).toXMLString();
		} else if (window.DOMParser) {
			var sheet ='<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/><xsl:template match="node()|@*"><xsl:copy><xsl:apply-templates select="node()|@*"/></xsl:copy></xsl:template></xsl:stylesheet>';
			var xsl = new DOMParser().parseFromString(sheet, "text/xml");
			var src = new DOMParser().parseFromString(text, "text/xml");
			var xsltproc = new XSLTProcessor();
			xsltproc.importStylesheet(xsl);
			var result = xsltproc.transformToDocument(src);
			if (result)
				return new XMLSerializer().serializeToString(result);
		}
		return text;
	}

	function updateBody() {
		var wym = window.WYMeditor.INSTANCES[0];
		$(wym._doc.body).find('.vbox:not(:has(p))').append('<p></p>');
		$(wym._doc.body).find('.hbox:not(:has(p))').append('<p></p>');
		$(wym._doc.body).find('.vbox>div+div').before('<p></p>');
		$(wym._doc.body).find('.hbox>div+div').before('<p></p>');
	}

	window.WYMeditor.INSTANCES[0].initHtml = function(html) {
		var wym = window.WYMeditor.INSTANCES[0];

		var innerHtml = wym.html;
		wym.html = function(html) {
			if(typeof html === 'string') {
				var ret = innerHtml.call(this, html);
				$(wym._doc.body).find(':input').attr("disabled", 'disabled');
				updateBody();
				return ret;
			} else {
				$(wym._doc.body).find(':input').removeAttr("disabled");
				var markup = $(this._doc.body).html();
				$(wym._doc.body).find(':input').attr("disabled", 'disabled');
				return markup;
			}
		};

		var innerXhtml = wym.xhtml;
		wym.xhtml = function(html) {
			var xhtml = innerXhtml.call(this, html);
			var formatted = formatXML('<body>' + xhtml + '</body>');
			return formatted.replace(/^\s*<body\s*>/, '').replace(/<\/body>\s*$/, '');
		};

		var innerKeyup = wym.keyup;
		wym.keyup = function(event) {
			var ret = innerKeyup.call(this, event);
			updateBody();
			return ret;
		};

		var innerContainer = wym.container;
		wym.container = function(sType) {
			if (sType && sType.indexOf('.') >= 0) {
				if (sType.indexOf('.') > 0) {
					innerContainer.call(this, sType.substring(0, sType.indexOf('.')));
				}
				var container = $(innerContainer.call(this));
				var css = sType.split('.');
				for (var i = 1; i < css.length; i++) {
					sType = container.toggleClass(css[i]);
				}
				return innerContainer.call(this);
			} else {
				return innerContainer.call(this, sType);
			}
		};

		var previously = wym.exec;
		var dialog_input = outter($('.wym_dialog_input'));
		var dialog_select = outter($('.wym_dialog_select'));
		var dialog_drop = outter($('.wym_dialog_dropzone'));
		var dialog_form = outter($('.wym_dialog_form'));
		var dialog_rel = outter($('.wym_dialog_rel'));

		wym.exec = function(cmd) {
			switch (cmd) {
			case 'InsertVBox':
				insertContainer.call(wym, '<div class="vbox">\n<p></p></div>\n', 'div.vbox');
			break;
			case 'InsertHBox':
				insertContainer.call(wym, '<div class="hbox">\n<p></p></div>\n', 'div.hbox');
			break;
			case 'InsertForm':
				insertForm.call(wym, dialog_form);
			break;
			case 'InsertInput':
				insertInput.call(wym, dialog_input, 'input', '<input id="$id" disabled="disabled" value="{$curie}" />');
			break;
			case 'InsertTextArea':
				insertInput.call(wym, dialog_input, 'text', '<textarea id="$id" class="auto-expand" disabled="disabled">{$curie}</textarea>');
			break;
			case 'InsertRadio':
				insertSelect.call(wym, dialog_select, 'radio', '<div id="$id">\n<label $rel="$curie" resource="?$id"><input type="radio" name="$id" checked="checked" /><span rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" /></label>\n</div>');
			break;
			case 'InsertCheckbox':
				insertSelect.call(wym, dialog_select, 'checkbox', '<div id="$id">\n<label $rel="$curie" resource="?$id"><input type="checkbox" name="$id" checked="checked" /><span rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" /></label>\n</div>');
			break;
			case 'InsertDropDown':
				insertSelect.call(wym, dialog_select, 'dropdown', '<select id="$id" $rel="$curie">\n<option about="?$id" rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" selected="selected" />\n</select>');
			break;
			case 'InsertSelect':
				insertSelect.call(wym, dialog_select, 'select', '<select multiple="multiple" id="$id" $rel="$curie">\n<option about="?$id" rel="skos:inScheme" resource="$scheme" property="skos:prefLabel" selected="selected" />\n</select>');
			break;
			case 'InsertDropZone':
				InsertDropZone.call(wym, dialog_drop, 'dropzone', '<div id="$id" class="$type field" $rel="$curie"><label>$label</label><button type="button" class="dialog" data-dialog="$prompt"/>\n<div about="?$id" typeof="$class"><span property="rdfs:label" /><button type="button" class="remove" /></div>\n</div>');
			break;
			case 'InsertRel':
				insertRel.call(wym, dialog_rel);
			break;
			default:
				var ret = previously.call(wym, cmd);
				wym.update();
				return ret;
			}
			wym.update();
		};

		jQuery.wymeditors(0)._html = html;
		wym.initIframe($('#iframe')[0]);
		$('#iframe').trigger('load');
		setTimeout(function(){
			if (!$('body', $('#iframe')[0].contentWindow.document).children().length) {
				// IE need this called twice on first load
				wym.initIframe($('#iframe')[0]);
				updateBody();
			}
		}, 1000);
	}
});
