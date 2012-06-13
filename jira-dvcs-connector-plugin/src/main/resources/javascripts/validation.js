function DvcsValidator () {
	
	this.validatedItems = [];
	this.valid = true;
	
}

DvcsValidator.clearAllErrors = function() {
	AJS.$(".dvcs-error").each(function(index, item) {
		AJS.$(item).hide();
	});
}

DvcsValidator.prototype.addItem = function (valuableInputId, errorElementId, rule) {
	
	this.validatedItems.push({
		"errorElementId" : errorElementId,
		"valuableInputId" : valuableInputId,
		"rule" : rule,
		"valid" : true
	});
	
}

DvcsValidator.prototype.runValidation = function () {
	
	DvcsValidator.clearAllErrors();
	
	for (index in this.validatedItems) {
		
		var value = this.validatedItems[index];

		// if field has more validators, skip other validations in case of first invalidity
		if (!value.valid) {
			continue;
		}
		
		var jqElement = AJS.$("#" + value.valuableInputId);
		
		// rule required
		if (value.rule == "required" && !jqElement.val()) {
			var jqElementError = AJS.$("#" + value.errorElementId);
			jqElementError.show();
			value.valid = false;
			
			this.valid = false;
		}
		// rule URL
		else if (value.rule == "url" && jqElement.val() && !dvcsIsUrl(jqElement.val())) {
			var jqElementError = AJS.$("#" + value.errorElementId);
			jqElementError.show();
			value.valid = false;
			
			this.valid = false;
		}

	}
	
	return this.valid;
	
}

function dvcsIsUrl(s) {
	var regexp = /(http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
	return regexp.test(s);
}