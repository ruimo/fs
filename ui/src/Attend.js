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
      showHelp: false
    };
  }
  
  msg = (key) => {
    return this.state.messages(key);
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
          { key: 'registerCompleted'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    const agentName = Cookies.get('agentName', {path: '/attend'});
    if (agentName === undefined) {
      this.setState({
        guide: this.msg('registerBeforeRecordGuide')
      });
    } else {
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
      Cookies.set("agentName", json[phase].agentName, {expires: 7, path: '/attend'});

      this.setState({
        records: json,
        tsv: '',
        globalError: '',
        message: ''
      });
    } else if (resp.status === 409) {
      this.setState({
        recordAlreadyExists: phase,
        globalError: '',
        message: ''
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
            <td className='agentName'>{startRec.agentName}</td>
            <td className='agentLevel'>{startRec.agentLevel}</td>
            <td className='lifetimeAp'>{startRec.lifetimeAp}</td>
            <td className='distanceWalked'>{startRec.distanceWalked}</td>
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
            <td className='agentName'>{endRec.agentName}</td>
            <td className='agentLevel'>{endRec.agentLevel}</td>
            <td className='lifetimeAp'>{endRec.lifetimeAp}</td>
            <td className='distanceWalked'>{endRec.distanceWalked}</td>
            <td className='createdAt'>{endRec.createdAt}</td>
          </tr>
        );
      }
    };

    return (
      <div className="attend">
        {message}
        {globalError}

        <nav className="panel">
          <p className="panel-heading">
            {this.state.siteName}
          </p>
          <div className="panel-block">
            <table className="table site-table">
              <tbody>
                <tr>
                  <td>{this.msg('dateTime')}</td>
                  <td>{this.state.heldOn}</td>
                </tr>
                <tr>
                  <td>{this.msg('timeZone')}</td>
                  <td>{this.state.timezone}</td>
                </tr>
              </tbody>
            </table>
            <div className="qr">
              <QRCode value={"https://fs.ruimo.com/attend/" + this.props.match.params.siteId} size="64"/>
            </div>
          </div>
          
          <div className="panel-block">
            <div className="wrapper">
              { this.state.guide !== '' ?
                <div className="notification is-info">
                  {this.state.guide}
                </div>:
                ""}

              <div id="agentRecordWrapper" className="wrapper">
                <label className="label" htmlFor="tsv">
                  <span>{this.msg('agentRecord')}</span>
                  <a href="#help" className="button is-info" onClick={(e) => this.showHelp()}>
                    <i className="far fa-question-circle"></i>
                  </a>
                </label>
                <textarea id="tsv" rows="10" placeholder={this.msg('pasteAgentRecord')}
                          value={this.state.tsv} onChange={(e) => this.setState({tsv: e.target.value})}/>
              </div>

              <div id="buttons">
                <a href="#registerBeforeRecord" className="button" disabled={this.state.tsv === ''}
                   onClick={(e) => this.registerRecord('START')}>
                  {this.msg('registerBeforeRecord')}
                </a>
                &nbsp;
                <a href="#registerAfterRecord" className="button" disabled={this.state.tsv === ''}
                   onClick={(e) => this.registerRecord('END')}>
                  {this.msg('registerAfterRecord')}
                </a>
              </div>
            </div>
          </div>
        </nav>

        { this.state.records.START === undefined && this.state.records.END === undefined ? "" :
          <table className="table">
            <thead>
              <tr>
                <th></th>
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
        <div className={cx("modal", {'is-active': this.state.recordAlreadyExists !== ''})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div>
              { this.state.recordAlreadyExists === 'START' ?
                this.msg('beforeRecordAlreadyExists'):
                this.msg('afterRecordAlreadyExists') }
            </div>
            <div className='dialogButtons'>
              <a href="#overwrite" className="button is-danger"
                 onClick={(e) => {this.overwriteRecord(this.state.recordAlreadyExists);}}>
                {this.msg('overwrite')}
              </a>&nbsp;
              <a href="#cancel" className="button" onClick={(e) => {this.cancelRegisterRecord(e);}}>
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
      </div>
    );
  }
}

export default withRouter(Attend);
