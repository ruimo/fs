import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './Attend.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';
import Cookies from 'js-cookie/src/js.cookie.js';
import QRCode from "qrcode.react"
import helpImage from'./images/help.png';

class Attend extends Component {
  constructor(props) {
    super(props);
    this.state = {
      message: '',
      globalError: '',
      siteName: '',
      messages: MessagesLoader.Empty,
      heldOn: '',
      timezone: '',
      tsv: '',
      records: {},
      recordAlreadyExists: '',
      guide: '',
      showHelp: false,
      clearAgentNameConfirm: false
    };
  }
  
  msg = (key) => {
    return this.state.messages(key);
  }

  retrieveAgentRecord = async(agentName) => {
    try {
      const resp = await fetch(
        "/registeredRecords?siteId=" + this.props.match.params.siteId + "&agentName=" + encodeURI(agentName)
      );
        
      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          records: json,
        });
      } else {
        console.log("error: " + resp.status);
        this.setState({
          globalError: this.msg('error.unknown')
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'error.unknown'},
          { key: 'dateTime'},
          { key: 'timeZone'},
          { key: 'registerBeforeRecord'},
          { key: 'registerAfterRecord'},
          { key: 'pasteAgentRecord'},
          { key: 'agentRecord'},
          { key: 'beforeRecordAlreadyExists'},
          { key: 'afterRecordAlreadyExists'},
          { key: 'cancel'},
          { key: 'overwrite'},
          { key: 'agentName'},
          { key: 'agentLevel'},
          { key: 'lifetimeAp'},
          { key: 'distanceWalked'},
          { key: 'createdAt'},
          { key: 'registerBeforeRecordGuide'},
          { key: 'registerAfterRecordGuide'},
          { key: 'registerCompleted'},
          { key: 'csvFormatError'},
          { key: 'showScore'},
          { key: 'abbrevFaction'},
          { key: 'clear'},
          { key: 'clearAgentNameGuide'},
          { key: 'clearAgentNameGuide2'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    const agentName = Cookies.get('agentName', {path: '/attend'});
    this.setState({
      agentName
    });
    if (agentName !== undefined) {
      this.retrieveAgentRecord(agentName);
    }

    try {
      const resp = await fetch("/attend?siteId=" + this.props.match.params.siteId);

      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          siteName: json.siteName,
          heldOn: json.heldOn,
          tsv: '',
          timezone: json.timezone
        });
      } else if (resp.status === 404) {
        const json = await resp.json();
        this.setState({
          globalError: json.errorMessage
        });
      } else {
        console.log("error: " + resp.status);
        this.setState({
          globalError: this.msg('error.unknown')
        });
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }
  }

  registerRecord = async(phase) => {
    const resp = await fetch(
      "/registerRecord", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Csrf-Token": "nocheck"
        },
        body: JSON.stringify({
          siteId: this.props.match.params.siteId,
          phase: phase,
          tsv: this.state.tsv,
          overwrite: false
        })
      }
    );

    if (resp.status === 200) {
      const json = await resp.json();
      console.log("json: " + JSON.stringify(json));
      const agentName = json[phase].agentName;
      Cookies.set("agentName", agentName, {expires: 7, path: '/attend'});

      this.setState({
        records: json,
        tsv: '',
        globalError: '',
        message: '',
        agentName
      });
    } else if (resp.status === 400) {
      this.setState({
        csvError: this.msg('csvFormatError'),
        message: ''
      });
    } else if (resp.status === 409) {
      const json = await resp.json();
      const agentName = json.agentName;

      this.retrieveAgentRecord(agentName);
      this.setState({
        recordAlreadyExists: phase,
        globalError: '',
        message: '',
        agentName
      });
    } else {
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }
  }

  cancelRegisterRecord = (e) => {
    this.setState({
      recordAlreadyExists: ''
    });
  }

  overwriteRecord = async(phase) => {
    const resp = await fetch(
      "/registerRecord", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Csrf-Token": "nocheck"
        },
        body: JSON.stringify({
          siteId: this.props.match.params.siteId,
          phase: phase,
          tsv: this.state.tsv,
          overwrite: true
        })
      }
    );

    if (resp.status === 200) {
      const json = await resp.json();
      Cookies.set("agentName", json[phase].agentName, {expires: 7, path: '/attend'});

      this.setState({
        recordAlreadyExists: '',
        tsv: '',
        records: json,
        globalError: '',
        message: ''
      });
    } else if (resp.status === 400) {
      this.setState({
        csvError: this.msg('csvFormatError'),
        message: ''
      });
    } else {
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }
  }

  showHelp = () => {
    this.setState({
      showHelp: true
    });
  }

  showScore = () => {
    this.props.history.push("/agentRecords/" + this.props.match.params.siteId);
  }

  shouldDisableStartRecordButton = () => {
    return this.state.tsv === '';
  }

  shouldDisableEndRecordButton = () => {
    return this.state.tsv === '';
  }

  shouldStartRecordButtonShown = () => {
    if (this.state.agentName === undefined) {
      return true;
    } else {
      if (this.state.records.START === undefined) {
        return true;
      } else {
        return false;
      }
    }
  }

  shouldEndRecordButtonShown = () => {
    if (this.state.agentName === undefined) {
      return false;
    } else {
      if (this.state.records.START === undefined) {
        return false;
      } else if (this.state.records.END === undefined) {
        return true;
      } else {
        return false;
      }
    }
  }

  notification = () => {
    if (this.state.agentName === undefined) {
      return (
        <div className="notification is-info">
        {this.msg('registerBeforeRecordGuide')}
        </div>
      );
    } else {
      if (this.state.records.START === undefined) {
        return (
          <div className="notification is-info">
            {this.msg('registerBeforeRecordGuide')}
          </div>
        );
      } if (this.state.records.END === undefined) {
        return (
          <div className="notification is-info">
            {this.msg('registerAfterRecordGuide')}
          </div>
        );
      } else {
        return (
          <div className="notification is-info">
          {this.msg('registerCompleted')}
          </div>
        );
      }
    }
  }

  clearAgentName = () => {
    Cookies.remove("agentName", {path: '/attend'});

    this.setState({
      agentName: undefined,
      clearAgentNameConfirm: false
    });
  }

  shouldShowAgentRecord = () => {
    if (this.state.agentName === undefined) return false;
    else return this.state.records.START !== undefined || this.state.records.END !== undefined;
  }

  render() {
    const message = this.state.message === '' ? "" :
          <article className="message is-info">
            <div className="message-body">{this.state.message}</div>
          </article>;

    const globalError = this.state.globalError === '' ? "" :
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>;
      
    const startRow = (rec) => {
      if (rec.START === undefined) return null;
      else {
        const startRec = rec.START;
        return (
          <tr>
            <td className='phase'>{this.msg('registerBeforeRecord')}</td>
            <td className='faction'>{startRec.faction}</td>
            <td className='agentName'>{startRec.agentName}</td>
            <td className='agentLevel'>{startRec.agentLevel}</td>
            <td className='lifetimeAp'>{Number(startRec.lifetimeAp).toLocaleString()}</td>
            <td className='distanceWalked'>{Number(startRec.distanceWalked).toLocaleString()}</td>
            <td className='createdAt'>{startRec.createdAt}</td>
          </tr>
        );
      }
    };

    const endRow = (rec) => {
      if (rec.END === undefined) return null;
      else {
        const endRec = rec.END;
        return (
          <tr>
            <td className='phase'>{this.msg('registerAfterRecord')}</td>
            <td className='faction'>{endRec.faction}</td>
            <td className='agentName'>{endRec.agentName}</td>
            <td className='agentLevel'>{endRec.agentLevel}</td>
            <td className='lifetimeAp'>{Number(endRec.lifetimeAp).toLocaleString()}</td>
            <td className='distanceWalked'>{Number(endRec.distanceWalked).toLocaleString()}</td>
            <td className='createdAt'>{endRec.createdAt}</td>
          </tr>
        );
      }
    };

    const csvError = this.state.csvError !== undefined ?
      <div className="error">
        { this.state.csvError }
      </div>
      : "";

    return (
      <div className="attend">
        {message}
        {globalError}

        <nav className="panel">
          <p className="panel-heading">
            {this.state.siteName}
          </p>
          <div className="panel-block">
            <div className="row0">
              <div>
                <table className="table site-table">
                  <tbody>
                    <tr>
                      <td>{this.msg('dateTime')}</td>
                      <td className="dateTime">{this.state.heldOn}</td>
                    </tr>
                    <tr>
                      <td>{this.msg('timeZone')}</td>
                      <td className="timezone">{this.state.timezone}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div className="qr">
                <QRCode value={"https://fs.ruimo.com/attend/" + this.props.match.params.siteId} size={64}/>
              </div>

              <div className="showScore">
                <a className="button is-info" href="#showScore" onClick={this.showScore}>{this.msg('showScore')}</a>
              </div>
            </div>
          </div>
          
          <div className="panel-block">
            <div className="wrapper">
              <div className="agentNameWrapper">
                <span className="header">{this.msg('agentName')}</span>
                <span className={cx("body tag is-info is-large", {'is-hidden': this.state.agentName === undefined})}>
                  {this.state.agentName}
                </span>
                <a href="#clearAgentName"
                   className={cx("clear button is-danger", {'is-hidden': this.state.agentName === undefined})}
                   onClick={() => this.setState({clearAgentNameConfirm: true})}>
                  {this.msg('clear')}
                </a>
              </div>
              { this.notification() }

              <div id="agentRecordWrapper" className="wrapper">
                <label className="label" htmlFor="tsv">
                  <span>{this.msg('agentRecord')}</span>
                  <a href="#help" className="button is-info" onClick={(e) => this.showHelp()}>
                    <i className="far fa-question-circle"></i>
                  </a>
                </label>
                <textarea id="tsv" rows="10" placeholder={this.msg('pasteAgentRecord')}
                          value={this.state.tsv} onChange={(e) => this.setState({tsv: e.target.value})}/>
                { csvError }
              </div>

              <div id="buttons">
                <a href="#registerBeforeRecord"
                   className={cx("button startRecord", {'is-hidden': !this.shouldStartRecordButtonShown()})}
                   onClick={(e) => this.registerRecord('START')} disabled={this.shouldDisableStartRecordButton()}>
                  {this.msg('registerBeforeRecord')}
                </a>
                &nbsp;
                <a href="#registerAfterRecord"
                   className={cx("button endRecord", {'is-hidden': !this.shouldEndRecordButtonShown()})}
                   onClick={(e) => this.registerRecord('END')} disabled={this.shouldDisableEndRecordButton()}>
                  {this.msg('registerAfterRecord')}
                </a>
              </div>
            </div>
          </div>
        </nav>

        { !this.shouldShowAgentRecord() ? "" :
          <table className="table records">
            <thead>
              <tr>
                <th></th>
                <th>{this.msg('abbrevFaction')}</th>
                <th>{this.msg('agentName')}</th>
                <th>{this.msg('agentLevel')}</th>
                <th>{this.msg('lifetimeAp')}</th>
                <th>{this.msg('distanceWalked')}</th>
                <th>{this.msg('createdAt')}</th>
              </tr>
            </thead>
            <tbody>
              {startRow(this.state.records)}
              {endRow(this.state.records)}
            </tbody>
          </table>
        }

        { /* Modals */ }
        <div className={cx("modal recordOverwriteConfirm", {'is-active': this.state.recordAlreadyExists !== ''})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div>
              { this.state.recordAlreadyExists === 'START' ?
                this.msg('beforeRecordAlreadyExists'):
                this.msg('afterRecordAlreadyExists') }
            </div>
            <div className='dialogButtons'>
              <a href="#cancel" className="button cancel" onClick={(e) => {this.cancelRegisterRecord(e);}}>
                {this.msg('cancel')}
              </a>
            </div>
            <div className={cx("notification is-danger errorMessage", {'is-active': this.state.globalError !== ''})}>
              {this.state.globalError}
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.cancelRegisterRecord(e);}}></button>
        </div>

        <div className={cx("modal", {'is-active': this.state.showHelp === true})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogButtons'>
              <div>
                <img alt="help" src={helpImage}/>
              </div>
              <div>
                <a href="#close-help" className="button close is-info"
                   onClick={(e) => {this.setState({showHelp: false});}}>
                  <i className="fas fa-times-circle"></i>
                </a>
              </div>
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.setState({showHelp: false});}}></button>
        </div>

        <div className={cx("modal clearAgentNameConfirm", {'is-active': this.state.clearAgentNameConfirm === true})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogButtons'>
              <div>
                <div>
                  {this.msg('clearAgentNameGuide')}
                </div>
                <div>
                  {this.msg('clearAgentNameGuide2')}
                </div>
              </div>
              <div>
                <a href="#close-agent-clear-confirm" className="button clear is-info"
                   onClick={(e) => {this.clearAgentName();}}>
                  {this.msg('clear')}
                </a>
                &nbsp;
                <a href="#close-agent-clear-confirm" className="button cancel is-info"
                   onClick={(e) => {this.setState({clearAgentNameConfirm: false});}}>
                  {this.msg('cancel')}
                </a>
              </div>
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.setState({showHelp: false});}}></button>
        </div>
      </div>
    );
  }
}

export default withRouter(Attend);
