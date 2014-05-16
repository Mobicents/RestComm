angular.module('Rvd')
.service('stepRegistry', function() {
	this.lastStepId = 0;
	this.name = function () {
		return 'step' + (++this.lastStepId);
	};
	this.reset = function (newIndex) {
		if (!newIndex)
			this.lastStepId = 0;
		else
			this.lastStepId = newIndex;
	}
	this.current = function() {
		return this.lastStepId;
	}
})
.service('stepPacker', ['$injector', function($injector) {
	this.unpack = function(source) {
		var unpacked = $injector.invoke([source.kind+'Model', function(model){
			var newStep = new model().init(source);
			return newStep;
		}]);
		return unpacked;
	}
}])
.factory('rvdModel', function () {
	function RvdModel() {
		this.test = function () {
			console.log('testing from RvdModel: ' + this.kind);
		}
		this.pack = function () {
			console.log("rvdModel:pack() - "  + this.name);
			var clone = angular.copy(this);
			return clone;
		}
		this.validate = function () {
			if (!this.iface)
			this.iface = {};
		}
		this.init = function (from) {
			angular.extend(this,from);
			return this;
		}
	}
	return RvdModel;
})
.factory('sayModel', ['rvdModel', function SayModelFactory(rvdModel) {
	function SayModel(name) {
		if (name)
			this.name = name;
		this.kind = 'say';
		this.label = 'say';
		this.title = 'say';
		this.phrase = '';
		this.voice = undefined;
		this.language = undefined;
		this.loop = undefined;
		this.iface = {};
	}

	SayModel.prototype = new rvdModel();
	SayModel.prototype.constructor = SayModel;
	// Add Say methods here
	// SayModel.prototype.method1 - function ()
	// ...
	
	return SayModel;
}])
.factory('playModel', ['rvdModel', function PlayModelFactory(rvdModel) {
	function PlayModel(name) {
		if (name)
			this.name = name;
		this.kind = 'play';
		this.label = 'play';
		this.title = 'play';
		this.loop = undefined;
		this.playType = 'local';
		this.local = {wavLocalFilename:''};
		this.remote = {wavUrl:''};
		this.iface = {};
	}
	PlayModel.prototype = new rvdModel();
	PlayModel.prototype.constructor = PlayModel; 
	PlayModel.prototype.validate = function() {
		if (!this.iface)
			this.iface = {};
		if (this.playType == "local")
			this.remote = {wavUrl:''};
		else if (this.playType == "remote")
			this.local = {wavLocalFilename:''};
	}
	PlayModel.prototype.pack = function () {
			if (this.playType == "local")
				delete this.remote;
			else if (this.playType == "remote")
				delete this.local;
	}
	
	return PlayModel;
}])
.factory('gatherModel', ['sayModel', 'rvdModel', function GatherModelFactory(sayModel, rvdModel) {
	function GatherModel(name) {
		if (name)
			this.name = name;
		this.kind = 'gather';
		this.label = 'gather';
		this.title = 'collect';
		this.action = undefined;
		this.method = 'GET';
		this.timeout = undefined;
		this.finishOnKey = undefined;
		this.numDigits = undefined;
		this.steps = [];
		this.validation = {messageStep: new sayModel(), pattern: "", iface:{userPattern:'', userPatternType:"One of"}};
		this.gatherType = "menu";
		this.menu = {mappings:[] }; //{digits:1, next:"welcome.step1"}
		this.collectdigits = {collectVariable:'',next:'', scope:"module"};
		this.iface = {}	;
	}	
	GatherModel.prototype = new rvdModel();
	GatherModel.prototype.constructor = GatherModel;
	GatherModel.prototype.validate = function() {
		if (!this.validation)
				this.validation = {messageStep: new sayModel(), pattern: "", iface:{userPattern:'', userPatternType:"One of"}};
		if (!this.validation.iface || angular.equals({},this.validation.iface) )
			this.validation.iface = {userPattern:this.validation.pattern, userPatternType:"Regex"};
		if (!this.menu)
			this.menu = {mappings:[] };
		if (!this.collectdigits)
			this.collectdigits = {collectVariable:'',next:'', scope:"module"};
	}
	GatherModel.prototype.pack = function () {
		console.log("gatherModel:pack() - " + this.name);
		var clone = angular.copy(this);
		if (clone.gatherType == "menu")
			delete clone.collectdigits;
		else
		if (clone.gatherType == "collectdigits")
			delete clone.menu;
		return clone;
	}
	return GatherModel;
}])
.factory('dialModel', ['rvdModel', 'numberNounModel', 'clientNounModel', 'conferenceNounModel', 'sipuriNounModel', function DialModelFactory(rvdModel, NumberNounModel, ClientNounModel, ConferenceNounModel, SipuriNounModel ) {
	function DialModel(name) {
		if (name)
			this.name = name;
		this.kind = 'dial';
		this.label = 'dial';
		this.title = 'dial';
		this.dialNouns = [];
		this.nextModule = undefined;
		this.action = undefined;
		this.method = undefined;
		this.timeout = undefined;
		this.timeLimit = undefined;
		this.callerId = undefined;
		this.record = undefined;
		this.iface = {};
	}
	DialModel.prototype = new rvdModel();
	DialModel.prototype.constructor = DialModel;	
	DialModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.dialNouns.length; i++) {
			var noun = from.dialNouns[i];
			if ( noun.dialType == 'number' )
				this.dialNouns[i] = new NumberNounModel().init(noun);
			else if ( noun.dialType == 'client' ) 
				this.dialNouns[i] = new ClientNounModel().init(noun);
			else if ( noun.dialType == 'conference' ) 
				this.dialNouns[i] = new ConferenceNounModel().init(noun);
			else if ( noun.dialType == 'sipuri' ) 
				this.dialNouns[i] = new SipuriNounModel().init(noun);
		}
		this.validate();
		return this;
	}
	
	return DialModel;
}])
.factory('numberNounModel',['rvdModel', function NumberNounModelFactory(rvdModel) {
	function NumberNounModel() {
		this.dialType = 'number';
		this.destination = '';
		this.sendDigits = undefined;
		this.beforeConnectModule = undefined;
	}
	NumberNounModel.prototype = new rvdModel();
	NumberNounModel.prototype.contructor = NumberNounModel;
	return NumberNounModel;
}])
.factory('clientNounModel',['rvdModel', function ClientNounModelFactory(rvdModel) {
	function ClientNounModel() {
		this.dialType = 'client';
		this.destination = '';
	}
	ClientNounModel.prototype = new rvdModel();
	ClientNounModel.prototype.contructor = ClientNounModel;
	return ClientNounModel;
}])
.factory('conferenceNounModel',['rvdModel', function ConferenceNounModelFactory(rvdModel) {
	function ConferenceNounModel() {
		this.dialType = 'conference';
		this.destination = '';
		this.nextModule = undefined;
		this.muted = undefined;
		this.beep = undefined;
		this.startConferenceOnEnter = undefined;
		this.endConferenceOnExit = undefined;
		this.waitUrl = undefined;
		this.waitModule = undefined;
		this.waitMethod = undefined;
		this.maxParticipants = undefined;
	}
	ConferenceNounModel.prototype = new rvdModel();
	ConferenceNounModel.prototype.contructor = ConferenceNounModel;
	return ConferenceNounModel;
}])
.factory('sipuriNounModel',['rvdModel', function SipuriNounModelFactory(rvdModel) {
	function SipuriNounModel() {
		this.dialType = 'sipuri';
		this.destination = '';
	}
	SipuriNounModel.prototype = new rvdModel();
	SipuriNounModel.prototype.contructor = SipuriNounModel;
	return SipuriNounModel;
}])
.factory('redirectModel', ['rvdModel', function RedirectModelFactory(rvdModel) {
	function RedirectModel(name) {
		if (name)
			this.name = name;
		this.kind = 'redirect';
		this.label = 'redirect';
		this.title = 'redirect';
		this.url  = null;
		this.method = null;
		this.iface = {};
	}
	RedirectModel.prototype = new rvdModel();
	RedirectModel.prototype.contructor = RedirectModel;
	return RedirectModel;
}])
.factory('hungupModel', ['rvdModel', function HungupModelFactory(rvdModel) {
	function HungupModel(name) {
		if (name)
			this.name = name;
		this.kind = 'hungup';
		this.label = 'hang up';
		this.title = 'hang up';
		this.iface = {};
	}
	HungupModel.prototype = new rvdModel();
	HungupModel.prototype.contructor = HungupModel;
	return HungupModel;
}])
.value('accessOperationKinds',['object','array','value'])
.value('objectActions', ['propertyNamed'])
.value('arrayActions', ['itemAtPosition'])
.factory('esValueExtractor',['rvdModel',function (rvdModel) {
	var accessOperationProtos = {
		object:{kind:'object',fixed:false, terminal:false},
		array:{kind:'array',fixed:false, terminal:false},
		value:{kind:'value',fixed:false, terminal:true}
	};
	function EsValueExtractor() {
		this.accessOperations = [];
		this.lastOperation = angular.copy( accessOperationProtos.object );
	}
	EsValueExtractor.prototype = new rvdModel();
	EsValueExtractor.prototype.constructor = EsValueExtractor;
	EsValueExtractor.prototype.addOperation = function () {
		console.log("adding operation");
		this.lastOperation.fixed = true;
		this.lastOperation.expression = this.operationExpression( this.lastOperation );
		this.accessOperations.push(this.lastOperation);
		this.lastOperation = angular.copy(accessOperationProtos.object)
	}
	EsValueExtractor.prototype.operationExpression = function (operation) {
		switch (operation.kind) {
		case 'object':
			switch (operation.action) {
			case 'propertyNamed':
				return "."+operation.property;
			}
		break;
		case 'array':
			switch (operation.action) {
			case 'itemAtPosition':
				return "[" + operation.position + "]";
			}
		break;
		case 'value':
			return " value";
		break;	
		}
		return "UNKNOWN";
	}
	EsValueExtractor.prototype.extractorModelExpression = function () {
		var expr = '';
		for ( var i=0; i < this.accessOperations.length; i++ ) {
			expr += this.operationExpression(this.accessOperations[i]);
		} 
		return expr;
	}
	EsValueExtractor.prototype.isTerminal = function (kind) {
		if (kind == null)
			return false;
		return accessOperationProtos[kind].terminal;
	}
	EsValueExtractor.prototype.doneAddingOperations = function () {
		this.addOperation();
		this.lastOperation = null;
	}
	EsValueExtractor.prototype.popOperation = function () { // removes last operation
		if ( this.accessOperations.length > 0 ) {
			this.lastOperation = this.accessOperations.pop();
			this.lastOperation.fixed = false;
		}
	}	
	return EsValueExtractor;
}])
.factory('esAssignment',['rvdModel','esValueExtractor',function (rvdModel,esValueExtractor) {
	function EsAssignment() {
		this.moduleNameScope = null;
		this.destVariable = '';
		this.scope = 'module';
		this.valueExtractor = new esValueExtractor(); 
	}
	EsAssignment.prototype = new rvdModel();
	EsAssignment.prototype.constructor = EsAssignment;
	EsAssignment.prototype.init = function(from) {
		angular.extend(this, from);
		this.valueExtractor = new esValueExtractor().init(from.valueExtractor);
		return this;
	}
	return EsAssignment;
}]) 
.factory('externalServiceModel', ['rvdModel','esAssignment','esValueExtractor', function ExternalServiceModelFactory(rvdModel,esAssignment,esValueExtractor) {
	function ExternalServiceModel(name) {
		if (name)
			this.name = name;
		this.kind = 'externalService';
		this.label = 'externalService';
		this.title = 'external service';
		this.url = '';
		this.urlParams = [];
		this.assignments = [];
		this.next = '';
		this.doRouting = false;
		this.nextType = 'fixed';
		this.nextValueExtractor = new esValueExtractor();
		this.iface = {};		
	}
	ExternalServiceModel.prototype = new rvdModel();
	ExternalServiceModel.prototype.contructor = ExternalServiceModel;
	ExternalServiceModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.assignments.length; i++) {
			var assignment = new esAssignment().init(from.assignments[i]);
			this.assignments[i] = assignment;
		}
		this.validate();
		return this;
	}
	ExternalServiceModel.prototype.addAssignment = function () {
		this.assignments.push(new esAssignment());
	}
	return ExternalServiceModel;
}])
.factory('rejectModel', ['rvdModel', function RejectModelFactory(rvdModel) {
	function RejectModel(name) {
		if (name)
			this.name = name;
		this.kind = 'reject';
		this.label = 'reject';
		this.title = 'reject';
		this.reason = undefined;
		this.iface = {};
	}
	RejectModel.prototype = new rvdModel();
	RejectModel.prototype.contructor = RejectModel;
	return RejectModel;
}])
.factory('pauseModel', ['rvdModel', function PauseModelFactory(rvdModel) {
	function PauseModel(name) {
		if (name)
			this.name = name;
		this.kind = 'pause';
		this.label = 'pause';
		this.title = 'pause';
		this.length = undefined;
		this.iface = {};
	}
	PauseModel.prototype = new rvdModel();
	PauseModel.prototype.contructor = PauseModel;
	return PauseModel;
}])

;
