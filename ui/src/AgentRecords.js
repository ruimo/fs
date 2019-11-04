import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';
import './Common.css';
import './AgentRecords.css';
import MessagesLoader from "./MessagesLoader";
import cx from 'classnames';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCaretSquareUp, faCaretSquareDown } from '@fortawesome/free-solid-svg-icons'

class AgentRecords extends Component {
  constructor(props) {
    super(props);
    this.state = {
      globalError: '',
      siteName: '',
      messages: MessagesLoader.Empty,
      records: [],
      pageControl: {
        currentPage: 0,
        pageSize: 10,
        pageCount: 0,
        nextPageExists: false,
        prevPageExists: false
      },
      deleteErrorMessage: '',
      onlyOrphanRecord: false
    };
  }

  msg = (key) => {
    return this.state.messages(key);
  }

  renderAgentRecords = async(page, orderBy, onlyOrphanRec) => {
    const siteId = this.props.match.params.siteId;
    const onlyOrphanRecord = onlyOrphanRec === undefined ? this.state.onlyOrphanRecord : onlyOrphanRec;

    try {
      const url = "/api/agentRecords?siteId=" + this.props.match.params.siteId +
            "&page=" + page +
            "&orderBySpec=" + encodeURI(orderBy) +
            "&mode=" + (onlyOrphanRecord ? 1: 0);
      const resp = await fetch(url);

      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          pageControl: json.pageControl,
          pagination: json.pagination,
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

    try {
      const resp = await fetch("/api/siteInfo?siteId=" + siteId)

      if (resp.status === 200) {
        const json = await resp.json();
        this.setState({
          siteInfo: json
        });
      } else if (resp.status === 404) {
        this.setState({
          siteInfo: undefined
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
          {key: 'recordEmpty'},
          {key: 'registerMyRecord'},
          {key: 'rank'},
          {key: 'scoreList'},
          {key: 'abbrevAgentName'},
          {key: 'abbrevFaction'},
          {key: 'abbrevStartLevel'},
          {key: 'abbrevEndLevel'},
          {key: 'abbrevEarnedLevel'},
          {key: 'abbrevStartAp'},
          {key: 'abbrevEndAp'},
          {key: 'abbrevEarnedAp'},
          {key: 'abbrevStartWalked'},
          {key: 'abbrevEndWalked'},
          {key: 'abbrevEarnedWalked'},
          {key: 'abbrevUpdatedTime'},
          {key: 'downloadWithTsv'},
          {key: 'all'},
          {key: 'cancel'},
          {key: 'clearAllAgentRecords'},
          {key: 'clearAgentRecord'},
          {key: 'clearAllAgentRecordsConfirm'},
          {key: 'clearAgentRecordConfirm'},
          {key: 'onlyOrphanRecord'},
          {key: 'abbrevLevel'},
          {key: 'abbrevAp'},
          {key: 'abbrevWalked'},
          {key: 'clearEndAgentRecord'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
      this.setState({
        globalError: this.msg('error.unknown')
      });
    }

    this.renderAgentRecords(0, "lifetime_ap_earned DESC");
  }

  registerMyRecord = () => {
    this.props.history.push("/attend/" + this.props.match.params.siteId);
  }

  nextPage = () => {
    this.renderAgentRecords(
      this.state.pageControl.currentPage + 1, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  prevPage = () => {
    this.renderAgentRecords(
      this.state.pageControl.currentPage - 1, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  gotoPage = (p) => {
    this.renderAgentRecords(
      p, this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
    );
  }

  renderOrderByNum = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      if (this.state.pageControl.orderBySort === "ASC") {
        return <span className="orderBy asc"><FontAwesomeIcon icon={faCaretSquareUp}/></span>;
      } else {
        return <span className="orderBy desc"><FontAwesomeIcon icon={faCaretSquareDown}/></span>;
      }
    } else {
      return <span className="orderBy"></span>;
    }
  }

  renderOrderByChar = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      if (this.state.pageControl.orderBySort === "ASC") {
        return <span className="orderBy asc"><FontAwesomeIcon icon={faCaretSquareUp}/></span>;
      } else {
        return <span className="orderBy desc"><FontAwesomeIcon icon={faCaretSquareDown}/></span>;
      }
    } else {
      return <span className="orderBy"></span>;
    }
  }

  sort = (colName) => {
    if (colName === this.state.pageControl.orderByCol) {
      const sortOrder = this.state.pageControl.orderBySort === "ASC" ? "DESC" : "ASC"
      this.renderAgentRecords(
        this.state.pageControl.currentPage, colName + " " + sortOrder
      );
    } else {
      this.renderAgentRecords(
        this.state.pageControl.currentPage, colName + " ASC"
      );
    }
  }

  clearAllAgentRecords = async(siteId) => {
    try {
      const resp = await fetch(
        "/api/deleteRecords?siteId=" + siteId, {
          method: "POST",
          headers: {
            "Csrf-Token": "nocheck"
          },
          body: ''
        }
      );

      if (resp.status === 200) {
        this.setState({
          clearAllAgentRecordsConfirm: false,
          records: [],
          globalError: '',
          deleteErrorMessage: ''
        });
      } else if (resp.status === 403) {
        this.setState({
          deleteErrorMessage: this.msg('unknownError')
        });
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  cancelClearAllAgentRecords = (e) => {
    this.setState({
      clearAllAgentRecordsConfirm: false
    });
  }

  showDeleteAllAgentRecordsDialog = () => {
    this.setState({
      clearAllAgentRecordsConfirm: true
    });
  }

  showDeleteAgentRecordDialog = (agentName) => {
    this.setState({
      clearAgentRecordAgentName: agentName
    });
  }

  cancelClearAgentRecord = (e) => {
    this.setState({
      clearAgentRecordAgentName: undefined
    });
  }

  clearAgentRecord = async(siteId, agentName, phase) => {
    try {
      const url = "/api/deleteAgentRecord?siteId=" + siteId +
            "&agentName=" + encodeURI(agentName) +
            (phase === undefined ? "" : "&phase=" + phase);

      const resp = await fetch(
        url, {
          method: "POST",
          headers: {
            "Csrf-Token": "nocheck"
          },
          body: ''
        }
      );

      if (resp.status === 200) {
        this.setState({
          clearAgentRecordAgentName: undefined,
          globalError: '',
          deleteErrorMessage: ''
        });
        this.renderAgentRecords(
          this.state.pageControl.currentPage,
          this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort
        );
      } else if (resp.status === 403) {
        this.setState({
          deleteErrorMessage: this.msg('unknownError')
        });
      }
    } catch (e) {
      console.log("error: " + JSON.stringify(e));
    }
  }

  onlyOrphanRecordChanged = (e) => {
    const onlyOrphanRecord = e.target.checked;

    this.setState({ onlyOrphanRecord });

    this.renderAgentRecords(
      0,
      onlyOrphanRecord ? "agent_name" : "lifetime_ap_earned DESC",
      onlyOrphanRecord
    );
  }

  createTable = () => {
    const canClearAgentRecord = 
          this.props.loginUser !== undefined &&
          this.state.siteInfo !== undefined &&
          this.state.siteInfo.ownerUserId === this.props.loginUser.id;

    const removeRecordHeader = 
          <th className="removeAllAgentRecords">
          { canClearAgentRecord ?
            <button className="is-danger button" title={this.msg("all")}
                    onClick={ev => this.showDeleteAllAgentRecordsDialog()}>
              {this.msg("all")}&nbsp;<i className="far fa-trash-alt"></i>
            </button>: <span className="placeHolder">&nbsp;</span>}
          </th>;

    const removeRecordRow = (row) => {
      return (
        <td className="removeRecord">
          { canClearAgentRecord ?
            <button className="is-danger button" title="removeAgentRecord"
                    onClick={ev => this.showDeleteAgentRecordDialog(row.agentName)}>
              <i className="far fa-trash-alt"></i>
            </button>: <span className="placeHolder">&nbsp;</span>}
        </td>
      );
    };

    return (
      this.state.records.length === 0 ?
        <span className="emptyMessage">{this.msg('recordEmpty')}</span>
        :
        <table className="table score normal">
          <thead>
            <tr>
              <th className="rank">{this.msg('rank')}</th>
              <th className={cx("agentName sortable", {"ordered": this.state.pageControl.orderByCol === "agent_name"})}
                  onClick={() => this.sort('agent_name')}>
                {this.renderOrderByChar('agent_name')}{this.msg('abbrevAgentName')}
              </th>
              <th className="faction">{this.msg('abbrevFaction')}</th>
              <th className="startLevel">{this.msg('abbrevStartLevel')}</th>
              <th className="endLevel">{this.msg('abbrevEndLevel')}</th>
              <th className="earnedLevel">{this.msg('abbrevEarnedLevel')}</th>
              <th className="startAp">{this.msg('abbrevStartAp')}</th>
              <th className="endAp">{this.msg('abbrevEndAp')}</th>
              <th className={cx("earnedAp sortable", {"ordered": this.state.pageControl.orderByCol === "lifetime_ap_earned"})}
                  onClick={() => this.sort('lifetime_ap_earned')}>
                {this.renderOrderByNum('lifetime_ap_earned')}{this.msg('abbrevEarnedAp')}
              </th>
              <th className="startWalked">{this.msg('abbrevStartWalked')}</th>
              <th className="endWalked">{this.msg('abbrevEndWalked')}</th>
              <th className={cx("earnedWalked sortable", {"ordered": this.state.pageControl.orderByCol === "distance_walked_earned"})}
                  onClick={() => this.sort('distance_walked_earned')}>
                {this.renderOrderByNum('distance_walked_earned')}{this.msg('abbrevEarnedWalked')}
              </th>
              <th className="updatedTime">{this.msg('abbrevUpdatedTime')}</th>
              {removeRecordHeader}
            </tr>
          </thead>
            
          <tbody>
            {
              this.state.records.map((e) =>
                <tr key={e.agentName}>
                  <td className="rank">{e.rank + 1}</td>
                  <td className="agentName">{e.agentName}</td>
                  <td className="faction">{e.faction}</td>
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
                  {removeRecordRow(e)}
                </tr>
              )
            }
          </tbody>
        </table>
    );
  }

  createOrphanTable = () => {
    const canClearAgentRecord = 
          this.props.loginUser !== undefined &&
          this.state.siteInfo !== undefined &&
          this.state.siteInfo.ownerUserId === this.props.loginUser.id;

    const removeRecordRow = (row) => {
      return (
        <td className="removeRecord">
          { canClearAgentRecord ?
            <button className="is-danger button" title="removeAgentRecord"
                    onClick={ev => this.showDeleteAgentRecordDialog(row.agentName)}>
              <i className="far fa-trash-alt"></i>
            </button>: <span className="placeHolder">&nbsp;</span>}
        </td>
      );
    };

    return (
      this.state.records.length === 0 ?
        <span className="emptyMessage">{this.msg('recordEmpty')}</span>
        :
        <table className="table score orphan">
          <thead>
            <tr>
              <th className={cx("agentName sortable", {"ordered": this.state.pageControl.orderByCol === "agent_name"})}
                  onClick={() => this.sort('agent_name')}>
                {this.renderOrderByChar('agent_name')}{this.msg('abbrevAgentName')}
              </th>
              <th className="faction">{this.msg('abbrevFaction')}</th>
              <th className="agentLevel">{this.msg('abbrevLevel')}</th>
              <th className="ap">{this.msg('abbrevAp')}</th>
              <th className="walked">{this.msg('abbrevWalked')}</th>
              <th className="updatedTime">{this.msg('abbrevUpdatedTime')}</th>
              <th></th>
            </tr>
          </thead>
            
          <tbody>
            {
              this.state.records.map((e) =>
                <tr key={e.agentName}>
                  <td className="agentName">{e.agentName}</td>
                  <td className="faction">{e.faction}</td>
                  <td className="agentLevel">{e.level}</td>
                  <td className="ap">{Number(e.ap).toLocaleString()}</td>
                  <td className="walked">{Number(e.walked).toLocaleString()}</td>
                  <td className="updatedTime">{e.createdAt}</td>
                  {removeRecordRow(e)}
                </tr>
              )
            }
          </tbody>
        </table>
    );
  }

  render() {
    const globalError = this.state.globalError === '' ? "" :
          <article className="message is-danger">
            <div className="message-body">{this.state.globalError}</div>
          </article>;

    const paginator = this.state.pagination === undefined ? "" :
          <nav className="pagination" role="navigation" aria-label="pagination">
            <a className="pagination-previous pagingButton" disabled={!this.state.pageControl.prevPageExists}
               onClick={this.prevPage} href="#prevPage">
              <i className="fas fa-chevron-circle-left fa-2x"></i>
            </a>
            <a className="pagination-next pagingButton" disabled={!this.state.pageControl.nextPageExists}
               onClick={this.nextPage} href="#nextPage">
              <i className="fas fa-chevron-circle-right fa-2x"></i>
            </a>
            <ul className="pagination-list">
              {this.state.pagination.topButtonExists ? (
                <li>
                  <a className="pagination-link top" aria-label="Goto top page" href="#gotoTopPage"
                     onClick={() => this.gotoPage(0)}>1</a>
                </li>
              ) : ""}
              {this.state.pagination.topButtonExists ? (
                <li>
                  <span className="pagination-ellipsis">&hellip;</span>
                </li>
              ) : ""}
              {(() => {
                const pages = [];
                for (let i = 0; i < this.state.pagination.showPageCount; ++i) {
                  const p = this.state.pagination.startPage + i + 1;
                  pages.push(
                    <li key={i}>
                      <a className={cx("pagination-link middle",{ "is-current": this.state.pageControl.currentPage + 1 === p})}
                         aria-label={"Goto page " + p} onClick={() => this.gotoPage(p - 1)} href="#gotoPage">{p}</a>
                    </li>
                  );
                }
                return pages;
              })()}
              {this.state.pagination.lastButtonExists ? (
                <li>
                  <span className="pagination-ellipsis">&hellip;</span>
                </li>
              ): ""}
              {this.state.pagination.lastButtonExists ? (
                <li>
                  <a className="pagination-link bottom" aria-label="Goto last page" href="#gotoLastPage"
                     onClick={() => this.gotoPage(this.state.pageControl.pageCount - 1)}>
                    {this.state.pageControl.pageCount}
                  </a>
                </li>
              ): ""}
            </ul>
          </nav>;

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
              <button className="button is-info" onClick={this.registerMyRecord}>
                {this.msg('registerMyRecord')}
              </button>
              &nbsp;
              <a href={"/api/downloadAgentRecords?siteId=" + this.props.match.params.siteId + "&orderBySpec=" + encodeURI(this.state.pageControl.orderByCol + " " + this.state.pageControl.orderBySort)}
                 className="button is-info" target="_blank" rel="noreferrer noopener">
                {this.msg('downloadWithTsv')}
              </a>
              &nbsp;
              <label className="checkbox" id="onlyOrphanRecord">
                <input type="checkbox" checked={this.state.onlyOrphanRecord} onChange={this.onlyOrphanRecordChanged}/>
                {this.msg("onlyOrphanRecord")}
              </label>
            </div>
          </div>

          <div className="panel-block">
            <div className="recordsWrapper">
              <div className="tableWrapper">
                {this.state.onlyOrphanRecord ? this.createOrphanTable() : this.createTable()}
              </div>
              <div className="paginatorWrapper">{paginator}</div>
            </div>
          </div>
        </nav>

        { /* modals */ }
        <div className={cx("modal clearAllAgentRecordsConfirm", {'is-active': this.state.clearAllAgentRecordsConfirm === true})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogSiteName'>
              { this.state.siteName !== undefined ? this.state.siteName : "" }
            </div>
            <div>
              { this.msg('clearAgentRecordConfirm') }
            </div>
            <div className='dialogButtons'>
              <button className="button is-danger clearAgentRecords"
                 onClick={(e) => {this.clearAllAgentRecords(this.props.match.params.siteId);}}>
                {this.msg('clearAllAgentRecords')}
              </button>&nbsp;
              <button className="button cancel" onClick={(e) => {this.cancelClearAllAgentRecords(e);}}>
                {this.msg('cancel')}
              </button>
            </div>
            <div className={cx("notification is-danger errorMessage", {'is-active': this.state.deleteErrorMessage !== ''})}>
              {this.state.deleteErrorMessage}
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.cancelClearAllAgentRecords(e);}}></button>
        </div>

        <div className={cx("modal clearAgentRecordConfirm", {'is-active': this.state.clearAgentRecordAgentName !== undefined})}>
          <div className="modal-background"></div>
          <div className="modal-content">
            <div className='dialogAgentName'>
              { this.state.clearAgentRecordAgentName }
            </div>
            <div>
              { this.msg('clearAgentRecordConfirm') }
            </div>
            <div className='dialogButtons'>
              <button className="button is-danger clearAgentRecord"
                      onClick={(e) => {this.clearAgentRecord(this.props.match.params.siteId, this.state.clearAgentRecordAgentName);}}>
                {this.msg('clearAgentRecord')}
              </button>&nbsp;
              <button className="button is-danger clearEndAgentRecord"
                      onClick={(e) => {this.clearAgentRecord(this.props.match.params.siteId, this.state.clearAgentRecordAgentName, 1);}}>
                {this.msg('clearEndAgentRecord')}
              </button>&nbsp;
              <button className="button cancel" onClick={(e) => {this.cancelClearAgentRecord(e);}}>
                {this.msg('cancel')}
              </button>
            </div>
            <div className={cx("notification is-danger errorMessage", {'is-active': this.state.deleteErrorMessage !== ''})}>
              {this.state.deleteErrorMessage}
            </div>
          </div>
          <button className="modal-close is-large" aria-label="close" onClick={(e) => {this.cancelClearAgentRecord(e);}}></button>
        </div>
      </div>
    );
  }
}

export default withRouter(AgentRecords);
