import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './AgentRecords.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';
import QRCode from "qrcode.react"

class AgentRecords extends Component {
  constructor(props) {
    super(props);
    this.state = {
      globalError: '',
      siteName: '',
      messages: MessagesLoader.Empty,
      records: []
    };
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          {key: 'recordEmpty'},
          {key: 'registerMyRecord'},
          {key: 'rank'},
          {key: 'scoreList'},
          {key: 'abbrevAgentName'},
          {key: 'abbrevStartLevel'},
          {key: 'abbrevEndLevel'},
          {key: 'abbrevEarnedLevel'},
          {key: 'abbrevStartAp'},
          {key: 'abbrevEndAp'},
          {key: 'abbrevEarnedAp'},
          {key: 'abbrevStartWalked'},
          {key: 'abbrevEndWalked'},
          {key: 'abbrevEarnedWalked'},
          {key: 'abbrevUpdatedTime'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    try {
      const resp = await fetch("/agentRecords?siteId=" + this.props.match.params.siteId);

      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          siteName: json.site.siteName,
          records: json.table
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

  registerMyRecord = () => {
    this.props.history.push("/attend/" + this.props.match.params.siteId);
  }

  render() {
    const globalError = this.state.globalError === '' ? "" :
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>;

    const table = this.state.records.length === 0 ?
          <span className="emptyMessage">{this.msg('recordEmpty')}</span>
          :
          <table className="table">
            <thead>
              <tr>
                <th className="rank">{this.msg('rank')}</th>
                <th className="agentName">{this.msg('abbrevAgentName')}</th>
                <th className="startLevel">{this.msg('abbrevStartLevel')}</th>
                <th className="endLevel">{this.msg('abbrevEndLevel')}</th>
                <th className="earnedLevel">{this.msg('abbrevEarnedLevel')}</th>
                <th className="startAp">{this.msg('abbrevStartAp')}</th>
                <th className="endAp">{this.msg('abbrevEndAp')}</th>
                <th className="earnedAp">{this.msg('abbrevEarnedAp')}</th>
                <th className="startWalked">{this.msg('abbrevStartWalked')}</th>
                <th className="endWalked">{this.msg('abbrevEndWalked')}</th>
                <th className="earnedWalked">{this.msg('abbrevEarnedWalked')}</th>
                <th className="updatedTime">{this.msg('abbrevUpdatedTime')}</th>
              </tr>
            </thead>
            
            <tbody>
              {
                this.state.records.map((e) =>
                  <tr key={e.agentName}>
                    <td className="rank">{e.rank + 1}</td>
                    <td className="agentName">{e.agentName}</td>
                    <td className="startLevel">{e.startLevel}</td>
                    <td className="endLevel">{e.endLevel}</td>
                    <td className="earnedLevel">{e.earnedLevel}</td>
                    <td className="startAp">{Number(e.startAp).toLocaleString()}</td>
                    <td className="endAp">{Number(e.endAp).toLocaleString()}</td>
                    <td className="earnedAp">{Number(e.earnedAp).toLocaleString()}</td>
                    <td className="startWalked">{Number(e.startWalked).toLocaleString()}</td>
                    <td className="endWalked">{Number(e.endWalked).toLocaleString()}</td>
                    <td className="earnedWalked">{Number(e.earnedWalked).toLocaleString()}</td>
                    <td className="updatedTime">{e.createdAt}</td>
                  </tr>
                )
              }
            </tbody>
          </table>;

    return (
      <div className="attend">
        {globalError}

        <nav className="panel">
          <p className="panel-heading">
            {this.msg('scoreList')}
          </p>

          <div className="panel-block">
            <div className="siteNameWrapper">
              <span className="siteName">{this.state.siteName}</span>
              <a href="#registerAgentRecord" className="button is-info" onClick={this.registerMyRecord}>
                {this.msg('registerMyRecord')}
              </a>
            </div>
          </div>

          <div className="panel-block">
            {table}
          </div>
        </nav>
      </div>
    );
  }
}

export default withRouter(AgentRecords);
