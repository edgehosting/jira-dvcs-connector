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
	
	
	for (index in this.validatedItems) {

		var value = this.validatedItems[index];
		
		var jqElement = AJS.$("#" + value.errorElementId);
		
		// rule required
		if (value.rule == "required" && !jqElement.val()) {
			var jqElementError = AJS.$("#" + value.errorElementId);
			jqElementError.show();
			value.valid = false;
			
			this.valid = false;
		}

	}
	
	return this.valid;
	
}